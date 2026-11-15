import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, test, vi } from "vitest";
import SignInPage from "./SignInPage";

function renderPage(props = {}) {
  const handlers = {
    signIn: vi.fn().mockResolvedValue(undefined),
    onSignedIn: vi.fn(),
    onSwitchToSignUp: vi.fn(),
    ...props,
  };
  render(<SignInPage {...handlers} />);
  return handlers;
}

async function fillAndSubmit(user, email, password) {
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Sign in" }));
}

test("has an email field, a password field and a Sign in button", () => {
  renderPage();

  expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  expect(screen.getByLabelText("Email")).toHaveAttribute("type", "email");
  expect(screen.getByLabelText("Password")).toHaveAttribute("type", "password");
  expect(screen.getByLabelText("Email")).toBeRequired();
  expect(screen.getByLabelText("Password")).toBeRequired();
});

test("signs in with what was typed, then reports success", async () => {
  const user = userEvent.setup();
  const { signIn, onSignedIn } = renderPage();

  await fillAndSubmit(user, "  fan@example.com ", "secret");

  expect(signIn).toHaveBeenCalledWith("fan@example.com", "secret");
  expect(onSignedIn).toHaveBeenCalledTimes(1);
});

test("shows the reason sign-in failed and lets the user try again", async () => {
  const user = userEvent.setup();
  const { onSignedIn } = renderPage({
    signIn: vi.fn().mockRejectedValue(new Error("Invalid login credentials")),
  });

  await fillAndSubmit(user, "fan@example.com", "wrong");

  expect(await screen.findByRole("alert")).toHaveTextContent("Invalid login credentials");
  expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  expect(onSignedIn).not.toHaveBeenCalled();
});

test("disables the button while signing in", async () => {
  const user = userEvent.setup();
  renderPage({ signIn: vi.fn(() => new Promise(() => {})) });

  await fillAndSubmit(user, "fan@example.com", "secret");

  expect(screen.getByRole("button", { name: "Please wait…" })).toBeDisabled();
});

test("shows why the user was sent here", () => {
  renderPage({ notice: "Your session has expired. Please sign in again." });

  expect(screen.getByRole("status")).toHaveTextContent("Your session has expired");
});

test("links to sign-up", async () => {
  const user = userEvent.setup();
  const { onSwitchToSignUp } = renderPage();

  await user.click(screen.getByRole("button", { name: "Create an account" }));

  expect(onSwitchToSignUp).toHaveBeenCalled();
});
