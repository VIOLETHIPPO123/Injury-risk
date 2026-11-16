import { API_BASE_URL } from "./apiFetch";
import { saveSession } from "./session";

// The view to show after Google sign-in. The browser leaves the app and comes back with a fresh
// page load in between, so it's kept in sessionStorage rather than in React state.
const RETURN_TO_KEY = "sidelined.returnTo";

// Sends the browser to our backend's /auth/google, which forwards it to Supabase and Google.
// The frontend never sees the Supabase URL. `navigate` is swappable in tests.
export function startGoogleSignIn(returnTo, navigate = (url) => window.location.assign(url)) {
  try {
    if (returnTo) sessionStorage.setItem(RETURN_TO_KEY, returnTo);
  } catch {
    // Storage blocked: the user just lands on the homepage afterwards.
  }
  navigate(`${API_BASE_URL}/auth/google`);
}

function takeReturnTo() {
  try {
    const returnTo = sessionStorage.getItem(RETURN_TO_KEY);
    sessionStorage.removeItem(RETURN_TO_KEY);
    return returnTo;
  } catch {
    return null;
  }
}

// Run once at start-up, before App renders. When the browser has just come back from Google
// sign-in, Supabase has put the session in the URL fragment (#access_token=...&expires_at=...), or
// an error (#error=...&error_description=...) if it failed. The token is stored exactly like an
// email sign-in's, so /me and every other request work the same way, and then the token or error is
// removed from the address bar (and from history, so Back doesn't bring it back).
// Returns null (an ordinary page load), { signedIn: true, returnTo } or { error }.
export function consumeOAuthRedirect(location = window.location, history = window.history) {
  const fragment = new URLSearchParams(location.hash.replace(/^#/, ""));
  const query = new URLSearchParams(location.search);
  const accessToken = fragment.get("access_token");
  const error =
    fragment.get("error_description") ??
    fragment.get("error") ??
    query.get("error_description") ??
    query.get("error");
  if (!accessToken && !error) return null;

  for (const key of ["error", "error_code", "error_description"]) query.delete(key);
  const search = query.toString();
  history.replaceState(history.state, "", location.pathname + (search ? `?${search}` : ""));

  const returnTo = takeReturnTo();
  if (!accessToken) return { error };

  saveSession({
    access_token: accessToken,
    expires_at: Number(fragment.get("expires_at")) || undefined,
    expires_in: Number(fragment.get("expires_in")) || undefined,
  });
  return { signedIn: true, returnTo };
}
