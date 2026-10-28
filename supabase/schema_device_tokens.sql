-- Device tokens for Chilavu push notifications.
-- Run in the Supabase SQL editor after schema.sql.

create table if not exists public.device_tokens (
    token text primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    updated_at timestamptz not null default now()
);

create index if not exists device_tokens_user_id_idx on public.device_tokens (user_id);

alter table public.device_tokens enable row level security;

drop policy if exists device_tokens_select_own on public.device_tokens;
create policy device_tokens_select_own on public.device_tokens
    for select using (user_id = auth.uid());

drop policy if exists device_tokens_insert_own on public.device_tokens;
create policy device_tokens_insert_own on public.device_tokens
    for insert with check (user_id = auth.uid());

drop policy if exists device_tokens_update_own on public.device_tokens;
create policy device_tokens_update_own on public.device_tokens
    for update using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists device_tokens_delete_own on public.device_tokens;
create policy device_tokens_delete_own on public.device_tokens
    for delete using (user_id = auth.uid());
