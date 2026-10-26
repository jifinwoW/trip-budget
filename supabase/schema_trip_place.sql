-- Trip place coordinates for header weather.
-- Run in: Supabase Dashboard → SQL Editor → New query → Run

alter table public.trips add column if not exists place_lat double precision;
alter table public.trips add column if not exists place_lng double precision;
