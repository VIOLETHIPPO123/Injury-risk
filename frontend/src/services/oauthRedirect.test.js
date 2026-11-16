import { beforeEach, describe, expect, test, vi } from "vitest";
import { consumeOAuthRedirect, startGoogleSignIn } from "./oauthRedirect";
import { loadSession } from "./session";

const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;

// A stand-in for window.location and window.history on the page Supabase sends the browser back to.
function landingPage({ hash = "", search = "", pathname = "/" } = {}) {
  return {
    location: { hash, search, pathname },
    history: { state: null, replaceState: vi.fn() },
  };
}

beforeEach(() => {
  localStorage.clear();
  sessionStorage.clear();
});

describe("startGoogleSignIn", () => {
  test("sends the browser to our backend, never to Supabase directly", () => {
    const navigate = vi.fn();

    startGoogleSignIn("my-roster", navigate);

    expect(navigate).toHaveBeenCalledWith("http://localhost:8080/auth/google");
  });

  test("remembers where to come back to across the page load", () => {
    startGoogleSignIn("my-roster", vi.fn());

    const { location, history } = landingPage({ hash: `#access_token=abc&expires_at=${inOneHour()}` });
    expect(consumeOAuthRedirect(location, history).returnTo).toBe("my-roster");
    // Used once only.
    expect(sessionStorage.getItem("sidelined.returnTo")).toBeNull();
  });
});

describe("consumeOAuthRedirect", () => {
  test("stores the token from the URL fragment, like an email sign-in", () => {
    const expiresAt = inOneHour();
    const { location, history } = landingPage({
      hash: `#access_token=abc123&expires_at=${expiresAt}&expires_in=3600&refresh_token=r&token_type=bearer&type=signup`,
    });

    const result = consumeOAuthRedirect(location, history);

    expect(result).toEqual({ signedIn: true, returnTo: null });
    expect(loadSession()).toEqual({ accessToken: "abc123", expiresAt });
  });

  test("removes the token from the address bar and from history", () => {
    const { location, history } = landingPage({
      pathname: "/",
      hash: `#access_token=abc123&expires_at=${inOneHour()}`,
    });

    consumeOAuthRedirect(location, history);

    // replaceState rewrites the current history entry, so Back doesn't bring the token back.
    expect(history.replaceState).toHaveBeenCalledWith(null, "", "/");
  });

  test("keeps unrelated query parameters while cleaning up", () => {
    const { location, history } = landingPage({
      pathname: "/app",
      search: "?utm=x",
      hash: `#access_token=abc123&expires_at=${inOneHour()}`,
    });

    consumeOAuthRedirect(location, history);

    expect(history.replaceState).toHaveBeenCalledWith(null, "", "/app?utm=x");
  });

  test("reports a failed Google sign-in, stores nothing and cleans the address bar", () => {
    const { location, history } = landingPage({
      search: "?error=access_denied&error_code=403&error_description=User+cancelled+the+login",
    });

    const result = consumeOAuthRedirect(location, history);

    expect(result).toEqual({ error: "User cancelled the login" });
    expect(loadSession()).toBeNull();
    expect(history.replaceState).toHaveBeenCalledWith(null, "", "/");
  });

  test("does nothing on an ordinary page load", () => {
    const { location, history } = landingPage({ search: "?q=1" });

    expect(consumeOAuthRedirect(location, history)).toBeNull();
    expect(history.replaceState).not.toHaveBeenCalled();
  });
});
