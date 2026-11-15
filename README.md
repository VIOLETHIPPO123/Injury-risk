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

On startup Hibernate creates or updates the `teams` and `players` tables and loads the 32 teams.
The `players` table stays empty until the player sync job (DEV-102) runs; to test before then,
add a few rows in the Supabase Table Editor.

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
