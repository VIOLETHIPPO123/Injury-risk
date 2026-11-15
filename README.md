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
The `players` table stays empty until the player sync job (DEV-102) runs; to test before then,
add a few rows in the Supabase Table Editor.

Tests and GitHub Actions don't need any of this: `./mvnw test` uses an in-memory H2 database
(the `test` profile in `backend/src/test/resources/application-test.properties`).
