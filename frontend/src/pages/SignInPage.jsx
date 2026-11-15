import AuthForm from "../components/AuthForm";
import { signIn as defaultSignIn } from "../services/authService";

// `notice` explains why the user is here (e.g. their session expired). `onSignedIn` runs once the
// token is stored. `signIn` is the service call, swappable in tests.
function SignInPage({ notice, onSignedIn, onSwitchToSignUp, signIn = defaultSignIn }) {
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
