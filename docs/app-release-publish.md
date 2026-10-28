# Publish a Chilavu update

1. In `app/build.gradle.kts`, raise `versionCode` and `versionName`.
2. Build the APK:

```bash
./gradlew :app:assembleDebug
```

3. Publish that file on GitHub:

```bash
gh release create v1.0.2 app/release/app-release.apk --title "1.0.2" --repo jifinwoW/trip-budget
```

4. In the SQL editor, record the same version and the release link:

```sql
insert into public.app_release (version_code, version_name, apk_url)
values (
    2,
    '1.0.1',
    'https://github.com/jifinwoW/trip-budget/releases/download/v1.0.1/app-debug.apk'
)
on conflict (version_code) do update
set version_name = excluded.version_name,
    apk_url = excluded.apk_url;
```
