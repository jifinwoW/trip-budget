-- Planned expenses for Budget Planning (run after schema_sharing.sql)
-- Supabase SQL Editor → Run once.

create table if not exists public.planned_expenses (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    title text not null,
    amount_paise bigint not null,
    category text not null,
    notes text,
    updated_at timestamptz not null default now()
);

create index if not exists planned_expenses_trip_id_idx on public.planned_expenses (trip_id);

alter table public.planned_expenses enable row level security;

drop policy if exists planned_expenses_select_access on public.planned_expenses;
create policy planned_expenses_select_access on public.planned_expenses
    for select using (public.can_access_trip(trip_id));

drop policy if exists planned_expenses_insert_access on public.planned_expenses;
create policy planned_expenses_insert_access on public.planned_expenses
    for insert with check (public.can_access_trip(trip_id));

drop policy if exists planned_expenses_update_access on public.planned_expenses;
create policy planned_expenses_update_access on public.planned_expenses
    for update using (public.can_access_trip(trip_id))
    with check (public.can_access_trip(trip_id));

drop policy if exists planned_expenses_delete_access on public.planned_expenses;
create policy planned_expenses_delete_access on public.planned_expenses
    for delete using (public.can_access_trip(trip_id));

-- Realtime (add in Dashboard → Replication if needed)
-- alter publication supabase_realtime add table public.planned_expenses;

