-- Names for people who join with an invite code.
-- Run in the SQL editor on a project that already has schema_sharing.sql.

alter table public.trip_access
    add column if not exists display_name text;

drop function if exists public.join_trip_by_code(text);

create or replace function public.join_trip_by_code(p_code text, p_name text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    v_trip_id uuid;
    v_code text := upper(trim(p_code));
    v_name text := trim(p_name);
begin
    if auth.uid() is null then
        raise exception 'Not signed in';
    end if;
    if v_code is null or length(v_code) < 4 then
        raise exception 'Enter a valid invite code';
    end if;
    if v_name is null or char_length(v_name) < 1 then
        raise exception 'Enter your name';
    end if;
    if char_length(v_name) > 40 then
        raise exception 'Keep your name under 40 characters';
    end if;

    select t.id into v_trip_id
    from public.trips t
    where t.invite_code = v_code
    limit 1;

    if v_trip_id is null then
        raise exception 'Invalid invite code';
    end if;

    insert into public.trip_access (trip_id, user_id, role, display_name)
    values (v_trip_id, auth.uid(), 'editor', v_name)
    on conflict (trip_id, user_id) do update
        set display_name = excluded.display_name
        where trip_access.role <> 'owner';

    return v_trip_id;
end;
$$;

revoke all on function public.join_trip_by_code(text, text) from public;
grant execute on function public.join_trip_by_code(text, text) to authenticated;

create or replace function public.trip_joiner_names(p_trip_id uuid)
returns table (display_name text)
language plpgsql
security definer
set search_path = public
as $$
begin
    if auth.uid() is null or not public.can_access_trip(p_trip_id) then
        return;
    end if;
    return query
    select coalesce(nullif(trim(ta.display_name), ''), 'Guest')
    from public.trip_access ta
    where ta.trip_id = p_trip_id
      and ta.role <> 'owner'
    order by ta.joined_at;
end;
$$;

revoke all on function public.trip_joiner_names(uuid) from public;
grant execute on function public.trip_joiner_names(uuid) to authenticated;
