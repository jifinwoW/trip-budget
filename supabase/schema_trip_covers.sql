-- Shared hero photo for a trip.
-- Bucket name in Storage must be: uploads
-- Leave the bucket private. These policies let trip members read and the owner write.
-- Run in: Supabase Dashboard → SQL Editor → New query → Run

alter table public.trips add column if not exists cover_path text;

create or replace function public.trip_id_from_cover_path(p_name text)
returns uuid
language plpgsql
stable
as $$
declare
    folder text;
begin
    folder := (storage.foldername(p_name))[1];
    if folder is null
        or folder !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' then
        return null;
    end if;
    return folder::uuid;
exception
    when others then
        return null;
end;
$$;

drop policy if exists trip_covers_select on storage.objects;
create policy trip_covers_select on storage.objects
    for select to authenticated
    using (
        bucket_id = 'uploads'
        and public.can_access_trip(public.trip_id_from_cover_path(name))
    );

drop policy if exists trip_covers_insert_owner on storage.objects;
create policy trip_covers_insert_owner on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'uploads'
        and public.is_trip_owner(public.trip_id_from_cover_path(name))
    );

drop policy if exists trip_covers_update_owner on storage.objects;
create policy trip_covers_update_owner on storage.objects
    for update to authenticated
    using (
        bucket_id = 'uploads'
        and public.is_trip_owner(public.trip_id_from_cover_path(name))
    )
    with check (
        bucket_id = 'uploads'
        and public.is_trip_owner(public.trip_id_from_cover_path(name))
    );

drop policy if exists trip_covers_delete_owner on storage.objects;
create policy trip_covers_delete_owner on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'uploads'
        and public.is_trip_owner(public.trip_id_from_cover_path(name))
    );
