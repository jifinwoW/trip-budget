-- Enable Supabase Realtime so the app stays live while open.
-- Run in SQL Editor after schema.sql (+ sharing). Safe to re-run:
-- if a table is already in the publication, that line errors — skip it.

alter publication supabase_realtime add table public.trips;
alter publication supabase_realtime add table public.members;
alter publication supabase_realtime add table public.contributions;
alter publication supabase_realtime add table public.expenses;
