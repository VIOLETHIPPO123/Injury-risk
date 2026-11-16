import { expect, test } from "vitest";

// The frontend only talks to our backend. A Supabase project URL or key in the source would ship
// to every browser, so none may appear in any frontend file.
const SOURCES = import.meta.glob("./**/*.{js,jsx,css,html}", {
  query: "?raw",
  import: "default",
  eager: true,
});

test("no frontend file contains a Supabase URL or key", () => {
  const offenders = Object.entries(SOURCES)
    .filter(([path]) => !path.endsWith("noSupabaseSecrets.test.js"))
    .filter(([, text]) => /supabase\.co|supabase_url|supabase_anon|anon[_-]?key|sb_publishable_|sb_secret_/i.test(text))
    .map(([path]) => path);

  expect(Object.keys(SOURCES).length).toBeGreaterThan(20);
  expect(offenders).toEqual([]);
});
