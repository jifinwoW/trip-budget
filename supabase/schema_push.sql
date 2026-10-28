-- pg_net delivers the webhook. Without it, triggers succeed and alerts are dropped.
create extension if not exists pg_net;

-- Push Chilavu alerts when the app is closed.
-- 1. Deploy supabase/functions/push (see README).
-- 2. Set the same random secret in the function and in Vault:
--      select vault.create_secret('paste-the-same-secret', 'push_webhook_secret');
-- 3. Run this file in the SQL editor.
-- If the pg_cron block notices that the extension is missing, enable pg_cron
-- and run just that block again. Weather checks run hourly. The 8:00 trip
-- reminders stay on the phone alarm.

create table if not exists public.trip_weather_state (
    trip_id uuid primary key references public.trips (id) on delete cascade,
    kind text not null,
    label text not null
);

alter table public.trip_weather_state enable row level security;

create or replace function public.enqueue_chilavu_push()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    secret text;
    payload jsonb;
begin
    if tg_table_name = 'route_stops' then
        if tg_op <> 'UPDATE'
            or new.status is distinct from 'REACHED'
            or old.status = 'REACHED' then
            return coalesce(new, old);
        end if;
    end if;
    if tg_table_name = 'trip_access' then
        if tg_op <> 'INSERT' or new.role = 'owner' then
            return coalesce(new, old);
        end if;
    end if;
    if tg_table_name = 'trip_announcements' and tg_op <> 'INSERT' then
        return coalesce(new, old);
    end if;

    begin
        select decrypted_secret into secret
        from vault.decrypted_secrets
        where name = 'push_webhook_secret'
        limit 1;
    exception
        when undefined_table then
            return coalesce(new, old);
    end;
    if secret is null or length(secret) = 0 then
        return coalesce(new, old);
    end if;

    payload := jsonb_build_object(
        'action', 'row',
        'table', tg_table_name,
        'op', tg_op,
        'actor', auth.uid(),
        'new', case when tg_op = 'DELETE' then null else to_jsonb(new) end,
        'old', case when tg_op = 'INSERT' then null else to_jsonb(old) end
    );

    begin
        perform net.http_post(
            url := 'https://offeviwwjjtbihjwgefy.supabase.co/functions/v1/push',
            body := payload,
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'x-push-secret', secret
            ),
            timeout_milliseconds := 15000
        );
    exception
        when others then
            raise warning 'chilavu push skipped: %', sqlerrm;
    end;
    return coalesce(new, old);
end;
$$;

revoke all on function public.enqueue_chilavu_push() from public;

create or replace function public.enqueue_chilavu_weather()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    secret text;
begin
    begin
        select decrypted_secret into secret
        from vault.decrypted_secrets
        where name = 'push_webhook_secret'
        limit 1;
    exception
        when undefined_table then
            return;
    end;
    if secret is null or length(secret) = 0 then
        return;
    end if;
    begin
        perform net.http_post(
            url := 'https://offeviwwjjtbihjwgefy.supabase.co/functions/v1/push',
            body := '{"action":"weather"}'::jsonb,
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'x-push-secret', secret
            ),
            timeout_milliseconds := 15000
        );
    exception
        when others then
            raise warning 'chilavu weather push skipped: %', sqlerrm;
    end;
end;
$$;

revoke all on function public.enqueue_chilavu_weather() from public;

drop trigger if exists expenses_push on public.expenses;
create trigger expenses_push
    after insert or update or delete on public.expenses
    for each row execute function public.enqueue_chilavu_push();

drop trigger if exists contributions_push on public.contributions;
create trigger contributions_push
    after insert or update or delete on public.contributions
    for each row execute function public.enqueue_chilavu_push();

drop trigger if exists members_push on public.members;
create trigger members_push
    after insert or update or delete on public.members
    for each row execute function public.enqueue_chilavu_push();

drop trigger if exists route_stops_push on public.route_stops;
create trigger route_stops_push
    after update on public.route_stops
    for each row execute function public.enqueue_chilavu_push();

drop trigger if exists trip_access_push on public.trip_access;
create trigger trip_access_push
    after insert on public.trip_access
    for each row execute function public.enqueue_chilavu_push();

drop trigger if exists trip_announcements_push on public.trip_announcements;
create trigger trip_announcements_push
    after insert on public.trip_announcements
    for each row execute function public.enqueue_chilavu_push();

do $cron$
declare
    existing bigint;
begin
    select jobid into existing from cron.job where jobname = 'chilavu-weather' limit 1;
    if existing is not null then
        perform cron.unschedule(existing);
    end if;
    perform cron.schedule(
        'chilavu-weather',
        '15 * * * *',
        'select public.enqueue_chilavu_weather()'
    );
exception
    when undefined_table or undefined_function or others then
        raise notice 'Enable the pg_cron extension, then re-run the weather schedule.';
end
$cron$;
