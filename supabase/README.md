# Supabase setup (Chilavu live cloud)

1. Open your project (region `ap-southeast-2`).
2. **SQL Editor** → run [`schema.sql`](schema.sql) first (if not already).
3. Then run [`schema_sharing.sql`](schema_sharing.sql) (invite codes + shared access).
   If you already ran sharing and see RLS errors on trip create, also run
   [`schema_sharing_fix_rls.sql`](schema_sharing_fix_rls.sql).
4. Run [`schema_realtime.sql`](schema_realtime.sql) so the app receives live updates.
   (If a table is already in the publication, that line errors — skip it.)
5. **Authentication → Providers**:
   - Enable **Email** (disable **Confirm email** for easier testing).
   - Enable **Anonymous** (used when someone joins with only an invite code).
6. **Settings → API**: copy Project URL and `anon` `public` key into the app’s `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=your_anon_public_key
```

Never put the `service_role` key in the Android app.

7. Rebuild the app.
   - Sign in (or join with a code) — data stays **live online**.
   - Edits upload immediately; Realtime + swipe-down refresh pull others’ changes.
   - Changing tabs refreshes from the cloud.
   - **Share trip** (⋮ menu) shows an invite code for friends.
   - Sign out clears the on-device cache and returns to Sign in.
