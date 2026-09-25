import RosterLink from "../components/RosterLink";
import "./AboutPage.css";

function AboutPage({ onBack, onNavigateToAcwr }) {
  return (
    <section className="about-page">
      <RosterLink onClick={onBack}>← Back to home</RosterLink>
      <h1>About Sidelined</h1>
      <p>
        Sidelined helps fantasy football managers spot hidden injury risk
        before it costs them a season, surfacing each player's workload-based
        risk score so keeper and trade decisions aren't a guessing game.
      </p>
      <RosterLink onClick={onNavigateToAcwr}>What is ACWR? →</RosterLink>
    </section>
  );
}

export default AboutPage;
