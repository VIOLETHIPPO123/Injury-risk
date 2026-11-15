import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, test, vi } from "vitest";
import SignUpPage from "./SignUpPage";

async function signUpAs(user, email, password) {
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Sign up" }));
}

test("a sign-up that signs straight in reports success", async () => {
  const user = userEvent.setup();
  const signUp = vi.fn().mockResolvedValue({ signedIn: true });
  const onSignedIn = vi.fn();
  render(<SignUpPage signUp={signUp} onSignedIn={onSignedIn} onSwitchToSignIn={vi.fn()} />);

  await signUpAs(user, "new@example.com", "secret123");

  expect(signUp).toHaveBeenCalledWith("new@example.com", "secret123");
  expect(onSignedIn).toHaveBeenCalled();
});

test("a sign-up that needs email confirmation says so instead", async () => {
  const user = userEvent.setup();
  const onSignedIn = vi.fn();
  render(
    <SignUpPage
      signUp={vi.fn().mockResolvedValue({ signedIn: false })}
      onSignedIn={onSignedIn}
      onSwitchToSignIn={vi.fn()}
    />,
  );

  await signUpAs(user, "new@example.com", "secret123");

  expect(await screen.findByRole("status")).toHaveTextContent("Check your email");
  expect(onSignedIn).not.toHaveBeenCalled();
});

test("shows why the account couldn't be made", async () => {
  const user = userEvent.setup();
  render(
    <SignUpPage
      signUp={vi.fn().mockRejectedValue(new Error("User already registered"))}
      onSignedIn={vi.fn()}
      onSwitchToSignIn={vi.fn()}
    />,
  );

  await signUpAs(user, "fan@example.com", "secret123");

  expect(await screen.findByRole("alert")).toHaveTextContent("User already registered");
});
