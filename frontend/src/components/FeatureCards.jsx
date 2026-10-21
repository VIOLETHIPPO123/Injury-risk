import "./FeatureCards.css";

// Simple line icons, drawn in the card's own color via currentColor.
const ICONS = {
  roster: (
    <path d="M4 6h16M4 12h16M4 18h10" />
  ),
  search: (
    <>
      <circle cx="11" cy="11" r="6" />
      <path d="m20 20-4.5-4.5" />
    </>
  ),
  watchlist: (
    <>
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  about: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 8h.01" />
    </>
  ),
};
const STAGGER_MS = 120;

// Colored shortcut cards for the homepage. Each rises in after `delay`, a step apart.
// The animation only moves and fades them, so they are clickable from the first frame.
function FeatureCards({ items, delay = 0 }) {
  return (
    <div className="feature-cards">
      {items.map(({ id, label, detail, onClick }, i) => (
        <button
          key={id}
          type="button"
          className={`feature-card feature-card-${id}`}
          style={{ animationDelay: `${delay + i * STAGGER_MS}ms` }}
          onClick={onClick}
        >
          <span className="feature-card-icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              {ICONS[id]}
            </svg>
          </span>
          <span className="feature-card-label">{label}</span>
          <span className="feature-card-detail">{detail}</span>
        </button>
      ))}
    </div>
  );
}

export default FeatureCards;
