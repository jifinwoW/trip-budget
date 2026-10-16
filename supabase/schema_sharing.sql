-- Trip sharing via invite codes (run AFTER schema.sql if not already applied)
-- Supabase SQL Editor → Run this file once.

-- ---------------------------------------------------------------------------
-- Invite code on trips + access roster
-- ---------------------------------------------------------------------------

alter table public.trips
    add column if not exists invite_code text;

update public.trips
set invite_code = upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6))
where invite_code is null;

alter table public.trips
    alter column invite_code set not null;

create unique index if not exists trips_invite_code_uidx on public.trips (invite_code);

create table if not exists public.trip_access (
    trip_id uuid not null references public.trips (id) on delete cascade,
    user_id uuid not null references auth.users (id) on delete cascade,
    role text not null check (role in ('owner', 'editor')),
    joined_at timestamptz not null default now(),
    primary key (trip_id, user_id)
);

create index if not exists trip_access_user_id_idx on public.trip_access (user_id);

-- Backfill owners into trip_access
insert into public.trip_access (trip_id, user_id, role)
select id, owner_id, 'owner'
from public.trips
on conflict do nothing;

-- Auto-add owner on new trip
create or replace function public.trips_after_insert_access()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.trip_access (trip_id, user_id, role)
    values (new.id, new.owner_id, 'owner')
    on conflict do nothing;
    return new;
end;
$$;

-- BEFORE insert to fill invite_code on the row itself
create or replace function public.trips_before_insert_invite()
returns trigger
language plpgsql
as $$
begin
    if new.invite_code is null or length(trim(new.invite_code)) = 0 then
        new.invite_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));
    end if;
    return new;
end;
$$;

drop trigger if exists trips_before_insert_invite on public.trips;
create trigger trips_before_insert_invite
    before insert on public.trips
    for each row execute function public.trips_before_insert_invite();

drop trigger if exists trips_after_insert_access on public.trips;
create trigger trips_after_insert_access
    after insert on public.trips
    for each row execute function public.trips_after_insert_access();

-- ---------------------------------------------------------------------------
-- Access helper
-- ---------------------------------------------------------------------------

create or replace function public.can_access_trip(p_trip_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.trip_access ta
        where ta.trip_id = p_trip_id
          and ta.user_id = auth.uid()
    );
$$;

create or replace function public.is_trip_owner(p_trip_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1
        from public.trips t
        where t.id = p_trip_id
          and t.owner_id = auth.uid()
    );
$$;

-- ---------------------------------------------------------------------------
-- Join by invite code
-- ---------------------------------------------------------------------------

create or replace function public.join_trip_by_code(p_code text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_trip_id uuid;
    v_code text := upper(trim(p_code));
begin
    if auth.uid() is null then
        raise exception 'Not signed in';
    end if;
    if v_code is null or length(v_code) < 4 then
        raise exception 'Enter a valid invite code';
    end if;

    select t.id into v_trip_id
    from public.trips t
    where t.invite_code = v_code
    limit 1;

    if v_trip_id is null then
        raise exception 'Invalid invite code';
    end if;

    insert into public.trip_access (trip_id, user_id, role)
    values (v_trip_id, auth.uid(), 'editor')
    on conflict (trip_id, user_id) do nothing;

    return v_trip_id;
end;
$$;

revoke all on function public.join_trip_by_code(text) from public;
grant execute on function public.join_trip_by_code(text) to authenticated;

-- ---------------------------------------------------------------------------
-- RLS: shared access (replace owner-only policies)
-- ---------------------------------------------------------------------------

alter table public.trip_access enable row level security;

drop policy if exists trip_access_select on public.trip_access;
create policy trip_access_select on public.trip_access
    for select using (user_id = auth.uid() or public.is_trip_owner(trip_id));

drop policy if exists trip_access_delete_self on public.trip_access;
create policy trip_access_delete_self on public.trip_access
    for delete using (
        user_id = auth.uid()
        and role <> 'owner'
    );

drop policy if exists trip_access_insert_owner on public.trip_access;
create policy trip_access_insert_owner on public.trip_access
    for insert with check (
        user_id = auth.uid()
        and (
            role = 'owner'
            or public.is_trip_owner(trip_id)
            or public.can_access_trip(trip_id)
        )
    );

-- Trips
drop policy if exists trips_select_own on public.trips;
drop policy if exists trips_select_access on public.trips;
-- owner_id check is required so INSERT…RETURNING works before/with trip_access
create policy trips_select_access on public.trips
    for select using (
        owner_id = auth.uid()
        or public.can_access_trip(id)
    );

drop policy if exists trips_insert_own on public.trips;
create policy trips_insert_own on public.trips
    for insert with check (owner_id = auth.uid());

drop policy if exists trips_update_own on public.trips;
drop policy if exists trips_update_access on public.trips;
create policy trips_update_access on public.trips
    for update using (public.can_access_trip(id))
    with check (public.can_access_trip(id));

create or replace function public.trips_prevent_owner_change()
returns trigger
language plpgsql
as $$
begin
    if new.owner_id is distinct from old.owner_id then
        raise exception 'Cannot change trip owner';
    end if;
    return new;
end;
$$;

drop trigger if exists trips_prevent_owner_change on public.trips;
create trigger trips_prevent_owner_change
    before update on public.trips
    for each row execute function public.trips_prevent_owner_change();

drop policy if exists trips_delete_own on public.trips;
create policy trips_delete_own on public.trips
    for delete using (owner_id = auth.uid());

-- Members / contributions / expenses — any trip accessor
drop policy if exists members_select_own on public.members;
drop policy if exists members_select_access on public.members;
create policy members_select_access on public.members
    for select using (public.can_access_trip(trip_id));

drop policy if exists members_insert_own on public.members;
drop policy if exists members_insert_access on public.members;
create policy members_insert_access on public.members
    for insert with check (public.can_access_trip(trip_id));

drop policy if exists members_update_own on public.members;
drop policy if exists members_update_access on public.members;
create policy members_update_access on public.members
    for update using (public.can_access_trip(trip_id));

drop policy if exists members_delete_own on public.members;
drop policy if exists members_delete_access on public.members;
create policy members_delete_access on public.members
    for delete using (public.can_access_trip(trip_id));

drop policy if exists contributions_select_own on public.contributions;
drop policy if exists contributions_select_access on public.contributions;
create policy contributions_select_access on public.contributions
    for select using (public.can_access_trip(trip_id));

drop policy if exists contributions_insert_own on public.contributions;
drop policy if exists contributions_insert_access on public.contributions;
create policy contributions_insert_access on public.contributions
    for insert with check (public.can_access_trip(trip_id));

drop policy if exists contributions_update_own on public.contributions;
drop policy if exists contributions_update_access on public.contributions;
create policy contributions_update_access on public.contributions
    for update using (public.can_access_trip(trip_id));

drop policy if exists contributions_delete_own on public.contributions;
drop policy if exists contributions_delete_access on public.contributions;
create policy contributions_delete_access on public.contributions
    for delete using (public.can_access_trip(trip_id));

drop policy if exists expenses_select_own on public.expenses;
drop policy if exists expenses_select_access on public.expenses;
create policy expenses_select_access on public.expenses
    for select using (public.can_access_trip(trip_id));

drop policy if exists expenses_insert_own on public.expenses;
drop policy if exists expenses_insert_access on public.expenses;
create policy expenses_insert_access on public.expenses
    for insert with check (public.can_access_trip(trip_id));

drop policy if exists expenses_update_own on public.expenses;
drop policy if exists expenses_update_access on public.expenses;
create policy expenses_update_access on public.expenses
    for update using (public.can_access_trip(trip_id));

drop policy if exists expenses_delete_own on public.expenses;
drop policy if exists expenses_delete_access on public.expenses;
create policy expenses_delete_access on public.expenses
    for delete using (public.can_access_trip(trip_id));
