-- Route stops for Trip Plan (run after schema_sharing.sql)
-- Supabase SQL Editor → Run once.

alter table public.trips
    add column if not exists route_tracking_active boolean not null default false;

create table if not exists public.route_stops (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    sort_order integer not null default 0,
    title text not null,
    subtitle text,
    lat double precision not null,
    lng double precision not null,
    osm_place_id text,
    status text not null check (status in ('UPCOMING', 'CURRENT', 'REACHED')),
    note text,
    updated_at timestamptz not null default now()
);

create index if not exists route_stops_trip_id_idx on public.route_stops (trip_id);
create index if not exists route_stops_trip_sort_idx on public.route_stops (trip_id, sort_order);

alter table public.route_stops enable row level security;

drop policy if exists route_stops_select_access on public.route_stops;
create policy route_stops_select_access on public.route_stops
    for select using (public.can_access_trip(trip_id));

drop policy if exists route_stops_insert_access on public.route_stops;
create policy route_stops_insert_access on public.route_stops
    for insert with check (public.can_access_trip(trip_id));

drop policy if exists route_stops_update_access on public.route_stops;
create policy route_stops_update_access on public.route_stops
    for update using (public.can_access_trip(trip_id))
    with check (public.can_access_trip(trip_id));

drop policy if exists route_stops_delete_access on public.route_stops;
create policy route_stops_delete_access on public.route_stops
    for delete using (public.can_access_trip(trip_id));

-- Realtime (also add in Dashboard → Replication if needed)
-- alter publication supabase_realtime add table public.route_stops;
