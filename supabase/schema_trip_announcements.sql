-- Owner messages that joined members receive as notifications.
-- Run in the Supabase SQL editor after schema_sharing.sql.

create table if not exists public.trip_announcements (
    id uuid primary key default gen_random_uuid(),
    trip_id uuid not null references public.trips (id) on delete cascade,
    sender_id uuid not null references auth.users (id) on delete cascade,
    title text not null,
    body text not null,
    created_at timestamptz not null default now()
);

create index if not exists trip_announcements_trip_id_idx
    on public.trip_announcements (trip_id);

alter table public.trip_announcements enable row level security;

drop policy if exists trip_announcements_select on public.trip_announcements;
create policy trip_announcements_select on public.trip_announcements
    for select using (public.can_access_trip(trip_id));

drop policy if exists trip_announcements_insert_owner on public.trip_announcements;
create policy trip_announcements_insert_owner on public.trip_announcements
    for insert with check (
        sender_id = auth.uid()
        and public.is_trip_owner(trip_id)
    );

alter publication supabase_realtime add table public.trip_announcements;
