import { clearSession, loadSession } from "./session";

export const API_BASE_URL = "http://localhost:8080";

// Thrown when the backend turns a request away for having no token or a bad or expired one.
// By then the user has been signed out and sent to sign-in, so callers should not show it.
export class SignedOutError extends Error {
  constructor() {
    super("Please sign in again.");
    this.name = "SignedOutError";
  }
}

let onSignedOut = () => {};

// App registers what to do when a request is turned away: show the sign-in page.
export function setSignedOutHandler(handler) {
  onSignedOut = handler ?? (() => {});
}

// fetch() for our backend: `path` is e.g. "/roster". When signed in, adds
// "Authorization: Bearer <token>". A 401 signs the user out (clears the stored token and
// calls the handler above) and throws SignedOutError instead of returning the response.
export async function apiFetch(path, options = {}) {
  const session = loadSession();
  const headers = { ...options.headers };
  if (session) headers.Authorization = `Bearer ${session.accessToken}`;

  const init = { ...options };
  if (Object.keys(headers).length > 0) init.headers = headers;

  const response = await fetch(`${API_BASE_URL}${path}`, init);
  if (response.status === 401) {
    clearSession();
    onSignedOut();
    throw new SignedOutError();
  }
  return response;
}
