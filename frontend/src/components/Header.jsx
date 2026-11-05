import StaggeredText from "./StaggeredText";
import "./Header.css";

const TITLE = "SIDELINED";
const TAGLINE = "Measure the Strain. Master the Game.";

// Title letters drop in, then the tagline rises word by word. The header stays mounted
// across views, so this plays once per page load. The title is a button back to the home
// page; it stays a button on the home page too (marked as the current page), because
// swapping the element would remount the letters and replay the animation.
function Header({ onHome, isHome = false }) {
  return (
    <header>
      <h1>
        <button
          type="button"
          className="header-home"
          aria-label={`${TITLE}, go to home page`}
          aria-current={isHome ? "page" : undefined}
          onClick={onHome}
        >
          <StaggeredText text={TITLE} variant="drop" step={70} />
        </button>
      </h1>
      <p>
        <StaggeredText
          text={TAGLINE}
          by="word"
          variant="rise"
          delay={600}
          step={120}
        />
      </p>
    </header>
  );
}

export default Header;
