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
