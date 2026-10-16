-- Fix: "new row violates row-level security policy for table trips"
-- Cause: PostgREST INSERT…RETURNING needs SELECT permission on the new row.
-- trip_access is filled by a trigger, but SELECT only used can_access_trip(),
-- which can fail on RETURNING right after insert.
--
-- Run this in SQL Editor once.

drop policy if exists trips_select_access on public.trips;
create policy trips_select_access on public.trips
    for select using (
        owner_id = auth.uid()
        or public.can_access_trip(id)
    );

-- Ensure owner is always on trip_access (security definer bypasses RLS)
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

drop trigger if exists trips_after_insert_access on public.trips;
create trigger trips_after_insert_access
    after insert on public.trips
    for each row execute function public.trips_after_insert_access();

-- Allow owners to insert their own access row (belt-and-suspenders)
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
