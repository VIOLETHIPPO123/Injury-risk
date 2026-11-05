import RosterLink from "../components/RosterLink";
import "./AboutPage.css";

function AboutPage({ onNavigateToAcwr }) {
  return (
    <section className="about-page">
      <h1>About Sidelined</h1>
      <p>
        Sidelined helps fantasy football managers spot hidden injury risk
        before it costs them a season, surfacing each player's workload-based
        risk score so keeper and trade decisions aren't a guessing game.
      </p>
      <div className="acwr-link">
        <RosterLink onClick={onNavigateToAcwr}>What is ACWR? →</RosterLink>
      </div>
    </section>
  );
}

export default AboutPage;
