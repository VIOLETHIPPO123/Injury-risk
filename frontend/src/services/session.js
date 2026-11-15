// The signed-in user's access token, kept in localStorage so a page reload stays signed in.
// Only our backend's token is stored here: the frontend never sees a Supabase URL or key.
const STORAGE_KEY = "sidelined.session";

// `signInResponse` is what POST /auth/signin (or a sign-up that signs straight in) returns:
// Supabase's { access_token, expires_at (seconds), expires_in (seconds), ... }.
export function saveSession(signInResponse) {
  const expiresAt =
    signInResponse.expires_at ?? Math.floor(Date.now() / 1000) + (signInResponse.expires_in ?? 0);
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({ accessToken: signInResponse.access_token, expiresAt }),
  );
}

// The stored token, or null when there is none or it has already expired (an expired token
// would only be turned away, so it counts as signed out).
export function loadSession() {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (!session?.accessToken || session.expiresAt * 1000 <= Date.now()) return null;
    return session;
  } catch {
    return null;
  }
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
}
