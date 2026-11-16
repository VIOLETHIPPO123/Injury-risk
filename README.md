# SIDELINED

## Backend setup: database environment variables

The backend connects to the team's Supabase PostgreSQL database. The connection details are
never committed: they come from three environment variables.

| Variable | What it is |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://<host>:5432/postgres?sslmode=require` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |

Get the values from the team privately (not over a public channel), then either:

- **Use a `.env` file (easiest):** copy `backend/.env.example` to `backend/.env` and fill it in.
  The backend reads `backend/.env` when you run it from the `backend` folder. `.env` is gitignored, so it
  never gets committed.
- **Or export them in your shell** before running. Real environment variables take precedence over `.env`.

```bash
cd backend
cp .env.example .env   # then fill in the values
./mvnw spring-boot:run
```

On startup Hibernate creates or updates the `teams`, `players`, `profiles`, `roster_entries`,
`user_roster_settings`, `roster_formats` and `roster_format_slots` tables. It loads the 32 teams and the
three roster formats, adding only what's missing, so a restart never duplicates them. A format already in
the table isn't updated from the code; to change one, edit or delete its row.

Each signed-in user has their own roster (`roster_entries.profile_id`) and their own format choice (one
`user_roster_settings` row, added the first time they pick a format; until then they're on ESPN Standard).

**Upgrading a database from before DEV-106:** Hibernate can't add the new `profile_id NOT NULL` column to a
`roster_entries` table that already has rows, because those rows have no owner. Before the first start on
DEV-106, in the Supabase SQL editor run `delete from roster_entries;` (old test data only) and, since nothing
uses it any more, `drop table roster_settings;`.
The `players` table is filled from [nflverse](https://github.com/nflverse/nflverse-data) by `PlayerSyncJob`:
everyone on a team's current depth chart, plus one `DEF` row per team. It runs every Tuesday at 2pm Central,
and at startup if the table is empty (the first start takes a few seconds longer and needs internet access).
Players are matched on `gsis_id`, so a run updates rows in place and never changes an id.

Right after it, `WorkloadSyncJob` loads the current season's snap counts from nflverse into `workload_entries`
(one row per player per week) and recalculates every player's two snap-count columns from them. It too runs at
startup if its table is empty; that first load is about 12,000 rows, so allow it a minute or two. A player needs
four games before they have an ACWR, on the player page and on the trend chart alike.

An admin can run both jobs at any time with the **Refresh player data** button next to Sign out, which calls
`POST /admin/players/refresh`. The button only shows for an admin, and the endpoint checks the role itself: a
signed-in non-admin gets 403 and a request with no token gets 401.

**Making an account an admin:** sign in with it once (that creates its `profiles` row), then in the Supabase
Table Editor open `profiles`, find the row whose `id` is the user's id (Authentication > Users lists them by
email) and change `role` from `USER` to `ADMIN`. Reload the app to see the button.

`InjuryReportSyncJob` loads nflverse's weekly NFL injury reports for the current season and the two before it into
`injury_reports` (one row per player per week: injury, game status, practice status). It runs every day at 2am
Central, and at startup (after the players) if the table is empty; that first load is about 15,000 rows. Reports
are matched to players on `gsis_id`. Reports for players not in `players` (mostly ones no longer on a depth chart)
aren't stored; each run logs how many there were.

Tests and GitHub Actions don't need any of this: `./mvnw test` uses an in-memory H2 database
(the `test` profile in `backend/src/test/resources/application-test.properties`).

## Google sign-in: one-time setup

"Sign in with Google" goes frontend → our backend (`GET /auth/google`) → Supabase → Google, then back to
the frontend with the session in the URL fragment. The frontend stores the token exactly like an email
sign-in and removes it from the address bar. A Google user's first request to `/me` creates their profile
as `USER`, and admin promotion and the 403 check work the same as for anyone.

The Google **client secret lives only in Supabase.** It is never in this repo, `.env`, the backend or the
frontend.

1. **Google Cloud Console** (console.cloud.google.com), APIs & Services:
   - OAuth consent screen: set it up (External), with the app name and a support email.
   - Credentials → Create credentials → **OAuth client ID** → type **Web application**.
   - Authorized redirect URI: `https://<project>.supabase.co/auth/v1/callback`
     (the Supabase project URL, as in `SUPABASE_URL`).
   - Copy the **Client ID** and **Client secret**.
2. **Supabase dashboard**:
   - Authentication → Sign In / Providers → **Google**: turn it on, paste the Client ID and Client secret, save.
   - Authentication → URL Configuration → **Redirect URLs**: add `http://localhost:5173`
     (and the deployed frontend URL later).
3. **Backend**: nothing to add for local runs. `FRONTEND_URL` (default `http://localhost:5173`) is where
   Supabase sends the browser back; set it when the frontend runs somewhere else, and add that URL to the
   Redirect URLs above too.
