import { useState } from "react";
import AuthForm from "../components/AuthForm";
import GoogleSignInButton from "../components/GoogleSignInButton";
import { signUp as defaultSignUp } from "../services/authService";

// If Supabase signs the new user straight in, `onSignedIn` runs. If it wants the email confirmed
// first, the page says so and points to sign-in. `signUp` is the service call, swappable in tests.
// `onGoogle` leaves for Google sign-in, which also makes the account on first use.
function SignUpPage({ onSignedIn, onSwitchToSignIn, onGoogle, signUp = defaultSignUp }) {
  const [awaitingConfirmation, setAwaitingConfirmation] = useState(false);

  function handleSubmit(email, password) {
    return signUp(email, password).then(({ signedIn }) => {
      if (signedIn) onSignedIn();
      else setAwaitingConfirmation(true);
    });
  }

  return (
    <section className="auth-page">
      <h1>Create an account</h1>
      {awaitingConfirmation ? (
        <p className="auth-notice" role="status">
          Check your email to confirm your account, then sign in.
        </p>
      ) : (
        <AuthForm submitLabel="Sign up" passwordAutoComplete="new-password" onSubmit={handleSubmit} />
      )}
      {onGoogle && <GoogleSignInButton onClick={onGoogle} />}
      <p className="auth-switch">
        Already have an account?{" "}
        <button type="button" onClick={onSwitchToSignIn}>
          Sign in
        </button>
      </p>
    </section>
  );
}

export default SignUpPage;
