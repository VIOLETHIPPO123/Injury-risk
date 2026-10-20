import StaggeredText from "./StaggeredText";

const TITLE = "SIDELINED";
const TAGLINE = "Measure the Strain. Master the Game.";

// Title letters drop in, then the tagline rises word by word. The header stays mounted
// across views, so this plays once per page load.
function Header() {
  return (
    <header>
      <h1>
        <StaggeredText text={TITLE} variant="drop" step={70} />
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
