import { Fragment } from "react";
import "./StaggeredText.css";

// Animates `text` in one piece at a time (letters or words), each starting `step` ms after
// the last. Screen readers get the text once, whole; the animated pieces are hidden from them.
function StaggeredText({
  text,
  by = "letter",
  variant = "drop",
  delay = 0,
  step = 60,
}) {
  const pieces = by === "word" ? text.split(" ") : [...text];

  return (
    <>
      <span className="visually-hidden">{text}</span>
      <span aria-hidden="true" data-testid="staggered-text">
        {pieces.map((piece, i) => (
          <Fragment key={i}>
            {by === "word" && i > 0 && " "}
            <span
              className={`staggered-piece staggered-${variant}`}
              style={{ animationDelay: `${delay + i * step}ms` }}
            >
              {piece}
            </span>
          </Fragment>
        ))}
      </span>
    </>
  );
}

export default StaggeredText;
