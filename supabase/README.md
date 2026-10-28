# Supabase setup (Chilavu live cloud)

1. Open your project (region `ap-southeast-2`).
2. **SQL Editor** → run [`schema.sql`](schema.sql) first (if not already).
3. Then run [`schema_sharing.sql`](schema_sharing.sql) (invite codes + shared access).
   If you already ran sharing and see RLS errors on trip create, also run
   [`schema_sharing_fix_rls.sql`](schema_sharing_fix_rls.sql).
   Run [`schema_join_names.sql`](schema_join_names.sql) so joiners can enter a name and the share sheet can list them.
4. Run [`schema_route_stops.sql`](schema_route_stops.sql) for the Plan tab (route stops + tracking).
5. Run [`schema_realtime.sql`](schema_realtime.sql) so the app receives live updates.
   (If a table is already in the publication, that line errors — skip it.)
6. Run [`schema_device_tokens.sql`](schema_device_tokens.sql) so this phone can store a notification token.
7. Run [`schema_trip_covers.sql`](schema_trip_covers.sql) so the trip hero photo can be stored in the private `uploads` bucket.
8. **Authentication → Providers**:
   - Enable **Email** (disable **Confirm email** for easier testing).
   - Enable **Anonymous** (used when someone joins with only an invite code).
9. **Settings → API**: copy Project URL and `anon` `public` key into the app’s `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=your_anon_public_key
```

Never put the `service_role` key in the Android app.

10. Push alerts while the app is closed. See [Push when Chilavu is closed](#push-when-chilavu-is-closed).
11. Run [`schema_app_release.sql`](schema_app_release.sql) so Settings can open the GitHub release when a newer version is published. See [Updates without the Play Store](#updates-without-the-play-store).
12. Rebuild the app.
   - Sign in (or join with a code) — data stays **live online**.
   - Edits upload immediately; Realtime + swipe-down refresh pull others’ changes.
   - Changing tabs refreshes from the cloud.
   - **Share trip** (⋮ menu) shows an invite code for friends.
   - Sign out clears the on-device cache and returns to Sign in.

## Push when Chilavu is closed

The phone stores its Firebase token in `device_tokens`. A database trigger calls the `push` edge function, which sends the alert through Firebase. The service-account key stays in Supabase secrets. Do not put it in the Android app.

1. Firebase console → Project settings → Service accounts → Generate new private key. Keep that JSON on this computer. Do not commit it.
2. In a terminal, from this project:

```bash
supabase secrets set FIREBASE_SERVICE_ACCOUNT="$(cat /path/to/service-account.json)"
supabase secrets set PUSH_WEBHOOK_SECRET="$(openssl rand -hex 32)"
supabase functions deploy push --no-verify-jwt
```

3. Copy the same `PUSH_WEBHOOK_SECRET` into the SQL editor:

```sql
select vault.create_secret('paste-the-same-secret', 'push_webhook_secret');
```

4. Run [`schema_push.sql`](schema_push.sql).
5. Open Chilavu once while signed in so this phone saves its token.

The 8:00 “Trip starts today” and “Next stop” reminders still use the phone alarm, so they can show when the app is not running. Force-stopping Chilavu in Android settings blocks every alert until the app is opened again.

## Updates without the Play Store

Publish steps are in [docs/app-release-publish.md](../docs/app-release-publish.md). Run [`schema_app_release.sql`](schema_app_release.sql) once before the first publish.
