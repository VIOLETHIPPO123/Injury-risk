import { API_BASE_URL, apiFetch } from "./apiFetch";
import { saveSession } from "./session";

// Sign-up and sign-in go to our backend's /auth endpoints, which forward to Supabase. The
// backend hands back Supabase's status and body unchanged, so errors come in Supabase's shapes.

// Supabase's error message, whichever shape it came in: { msg } (current), { error_description }
// (older), or our backend's own { error } (e.g. "Sign-in service unavailable").
function errorMessage(body, status) {
  return (
    body?.msg ??
    body?.error_description ??
    body?.message ??
    body?.error ??
    `Request failed with status ${status}`
  );
}

async function postCredentials(path, email, password) {
  // Plain fetch, not apiFetch: there is no token yet, and a rejected password isn't a lost session.
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  let body = null;
  try {
    body = await response.json();
  } catch {
    // No JSON body: the status alone will have to do.
  }
  if (!response.ok) throw new Error(errorMessage(body, response.status));
  return body;
}

// Signs in and stores the token. Rejects with Supabase's message, e.g. "Invalid login credentials".
export async function signIn(email, password) {
  saveSession(await postCredentials("/auth/signin", email, password));
}

// Creates the account. When Supabase signs the new user straight in, the token is stored and this
// resolves to { signedIn: true }. When the project asks new users to confirm their email first,
// there's no token yet and it resolves to { signedIn: false }.
export async function signUp(email, password) {
  const body = await postCredentials("/auth/signup", email, password);
  if (body?.access_token) {
    saveSession(body);
    return { signedIn: true };
  }
  return { signedIn: false };
}

// The signed-in user's role: "USER" or "ADMIN".
export async function getMyRole() {
  const response = await apiFetch("/me");
  if (!response.ok) throw new Error(`GET /me failed with status ${response.status}`);
  return (await response.json()).role;
}
