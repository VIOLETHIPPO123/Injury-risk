import "./Button.css";

function Button({ children, onClick, type = "button", ...rest }) {
  return (
    <button type={type} className="btn" onClick={onClick} {...rest}>
      {children}
    </button>
  );
}

export default Button;
