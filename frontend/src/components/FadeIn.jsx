import "./FadeIn.css";

// Fades its children in once, when it mounts. The animation is pure CSS: nothing waits on it,
// so wrapped buttons are clickable from the first frame. Opacity-only on purpose: a transform
// here would re-anchor position: fixed children (like the nav menu) to this wrapper.
function FadeIn({ children, delay = 0, className = "" }) {
  return (
    <div
      className={`fade-in ${className}`.trim()}
      style={{ animationDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
}

export default FadeIn;
