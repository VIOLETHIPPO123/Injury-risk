import { useState } from "react";
import "./AuthForm.css";

// The email and password form shared by the sign-in and sign-up pages. `onSubmit(email, password)`
// returns a promise; while it runs the button is disabled, and if it rejects its message is shown.
function AuthForm({ submitLabel, onSubmit, passwordAutoComplete }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    onSubmit(email.trim(), password)
      .catch((err) => setError(err.message))
      .finally(() => setSubmitting(false));
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <label htmlFor="auth-email">Email</label>
      <input
        id="auth-email"
        type="email"
        autoComplete="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      <label htmlFor="auth-password">Password</label>
      <input
        id="auth-password"
        type="password"
        autoComplete={passwordAutoComplete}
        required
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      {error && (
        <p className="auth-form-error" role="alert">
          {error}
        </p>
      )}

      <button type="submit" disabled={submitting}>
        {submitting ? "Please wait…" : submitLabel}
      </button>
    </form>
  );
}

export default AuthForm;
