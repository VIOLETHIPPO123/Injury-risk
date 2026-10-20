import "./HomeBackdrop.css";

const YARD_NUMBERS = ["10", "20", "30", "40", "50", "40", "30", "20", "10"];

// Decorative layers behind the homepage: a slow purple glow up top and a turf band
// with yard lines along the bottom. Pinned behind all content and never clickable.
function HomeBackdrop() {
  return (
    <div className="home-backdrop" aria-hidden="true" data-testid="home-backdrop">
      <div className="home-glow" />
      <div className="home-turf">
        <div className="home-turf-numbers">
          {YARD_NUMBERS.map((number, i) => (
            <span key={i}>{number}</span>
          ))}
        </div>
      </div>
    </div>
  );
}

export default HomeBackdrop;
