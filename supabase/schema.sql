-- Chilavu / Trip Budget Tracker — Supabase schema (v1 single-owner backup)
-- Run in: Supabase Dashboard → SQL Editor → New query → Run
-- Auth: Authentication → Providers → Email (disable "Confirm email" for easier testing)

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists public.trips (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users (id) on delete cascade,
    name text not null,
    destination text,
    place_lat double precision,
    place_lng double precision,
    planned_budget_paise bigint not null,
    start_date_epoch_day bigint,
    end_date_epoch_day bigint,
    created_at_millis bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at timestamptz not null default now()
);

create table if not exists public.members (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    name text not null,
    expected_contribution_paise bigint not null,
    updated_at timestamptz not null default now()
);

create index if not exists members_trip_id_idx on public.members (trip_id);

create table if not exists public.contributions (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    member_id uuid not null references public.members (id) on delete cascade,
    amount_paise bigint not null,
    paid_at_millis bigint not null,
    note text,
    updated_at timestamptz not null default now()
);

create index if not exists contributions_trip_id_idx on public.contributions (trip_id);
create index if not exists contributions_member_id_idx on public.contributions (member_id);

create table if not exists public.expenses (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    title text not null,
    amount_paise bigint not null,
    category text not null,
    date_epoch_day bigint not null,
    paid_by_member_id uuid references public.members (id) on delete cascade,
    payment_source text not null,
    notes text,
    updated_at timestamptz not null default now()
);

create index if not exists expenses_trip_id_idx on public.expenses (trip_id);
create index if not exists expenses_paid_by_member_id_idx on public.expenses (paid_by_member_id);

-- ---------------------------------------------------------------------------
-- Row Level Security — owner-only via trips.owner_id
-- ---------------------------------------------------------------------------

alter table public.trips enable row level security;
alter table public.members enable row level security;
alter table public.contributions enable row level security;
alter table public.expenses enable row level security;

-- Trips
drop policy if exists trips_select_own on public.trips;
create policy trips_select_own on public.trips
    for select using (owner_id = auth.uid());

drop policy if exists trips_insert_own on public.trips;
create policy trips_insert_own on public.trips
    for insert with check (owner_id = auth.uid());

drop policy if exists trips_update_own on public.trips;
create policy trips_update_own on public.trips
    for update using (owner_id = auth.uid()) with check (owner_id = auth.uid());

drop policy if exists trips_delete_own on public.trips;
create policy trips_delete_own on public.trips
    for delete using (owner_id = auth.uid());

-- Members
drop policy if exists members_select_own on public.members;
create policy members_select_own on public.members
    for select using (
        exists (
            select 1 from public.trips t
            where t.id = members.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists members_insert_own on public.members;
create policy members_insert_own on public.members
    for insert with check (
        exists (
            select 1 from public.trips t
            where t.id = members.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists members_update_own on public.members;
create policy members_update_own on public.members
    for update using (
        exists (
            select 1 from public.trips t
            where t.id = members.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists members_delete_own on public.members;
create policy members_delete_own on public.members
    for delete using (
        exists (
            select 1 from public.trips t
            where t.id = members.trip_id and t.owner_id = auth.uid()
        )
    );

-- Contributions
drop policy if exists contributions_select_own on public.contributions;
create policy contributions_select_own on public.contributions
    for select using (
        exists (
            select 1 from public.trips t
            where t.id = contributions.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists contributions_insert_own on public.contributions;
create policy contributions_insert_own on public.contributions
    for insert with check (
        exists (
            select 1 from public.trips t
            where t.id = contributions.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists contributions_update_own on public.contributions;
create policy contributions_update_own on public.contributions
    for update using (
        exists (
            select 1 from public.trips t
            where t.id = contributions.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists contributions_delete_own on public.contributions;
create policy contributions_delete_own on public.contributions
    for delete using (
        exists (
            select 1 from public.trips t
            where t.id = contributions.trip_id and t.owner_id = auth.uid()
        )
    );

-- Expenses
drop policy if exists expenses_select_own on public.expenses;
create policy expenses_select_own on public.expenses
    for select using (
        exists (
            select 1 from public.trips t
            where t.id = expenses.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists expenses_insert_own on public.expenses;
create policy expenses_insert_own on public.expenses
    for insert with check (
        exists (
            select 1 from public.trips t
            where t.id = expenses.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists expenses_update_own on public.expenses;
create policy expenses_update_own on public.expenses
    for update using (
        exists (
            select 1 from public.trips t
            where t.id = expenses.trip_id and t.owner_id = auth.uid()
        )
    );

drop policy if exists expenses_delete_own on public.expenses;
create policy expenses_delete_own on public.expenses
    for delete using (
        exists (
            select 1 from public.trips t
            where t.id = expenses.trip_id and t.owner_id = auth.uid()
        )
    );
