-- In-app updates. The APK is a public GitHub release asset.
-- This table only stores the version and that download link.

create table if not exists public.app_release (
    version_code integer primary key,
    version_name text not null,
    apk_url text,
    released_at timestamptz not null default now()
);

alter table public.app_release add column if not exists apk_url text;

alter table public.app_release enable row level security;

drop policy if exists app_release_select on public.app_release;
create policy app_release_select on public.app_release
    for select to anon, authenticated
    using (true);

drop policy if exists app_apk_select on storage.objects;
