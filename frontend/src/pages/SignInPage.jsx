import AuthForm from "../components/AuthForm";
import GoogleSignInButton from "../components/GoogleSignInButton";
import { signIn as defaultSignIn } from "../services/authService";

// `notice` explains why the user is here (e.g. their session expired). `onSignedIn` runs once the
// token is stored. `onGoogle` leaves for Google sign-in. `signIn` is the service call, swappable in tests.
function SignInPage({ notice, onSignedIn, onSwitchToSignUp, onGoogle, signIn = defaultSignIn }) {
  return (
    <section className="auth-page">
      <h1>Sign in</h1>
      {notice && (
        <p className="auth-notice" role="status">
          {notice}
        </p>
      )}
      <AuthForm
        submitLabel="Sign in"
        passwordAutoComplete="current-password"
        onSubmit={(email, password) => signIn(email, password).then(onSignedIn)}
      />
      {onGoogle && <GoogleSignInButton onClick={onGoogle} />}
      <p className="auth-switch">
        New here?{" "}
        <button type="button" onClick={onSwitchToSignUp}>
          Create an account
        </button>
      </p>
    </section>
  );
}

export default SignInPage;
