import { useState } from "react";
import { riskLevel, weeklyAcwr } from "../utils/acwr";
import { acwrTrend } from "../utils/acwrTrend";
import "./AcwrTrendChart.css";

// Drawing size in SVG units, about the player page's width, so text draws near its real
// size; the SVG scales to fit narrower screens.
const WIDTH = 480;
const HEIGHT = 220;
const PAD = { top: 12, right: 10, bottom: 28, left: 34 };
const PLOT_W = WIDTH - PAD.left - PAD.right;
const PLOT_H = HEIGHT - PAD.top - PAD.bottom;

// The risk levels from riskLevel, as bands behind the line. `to` of the top band is set
// by the chart's scale.
const BANDS = [
  { from: 0, to: 0.8, tier: "medium", label: "Ramping up" },
  { from: 0.8, to: 1.3, tier: "low", label: "Normal" },
  { from: 1.3, to: 1.5, tier: "medium", label: "Slightly elevated" },
  { from: 1.5, to: null, tier: "high", label: "Spiking" },
];
const Y_TICKS = [0, 0.8, 1.3, 1.5];

const format = (acwr) => (acwr === null ? "—" : acwr.toFixed(2));

// "Week 6: 62 snaps, ACWR 1.02"
function describeWeek({ week, snaps, acwr }) {
  return `Week ${week}: ${snaps} snaps, ACWR ${format(acwr)}`;
}

// Splits the weeks into runs of consecutive values, so the line breaks at a bye.
function lineSegments(points) {
  const segments = [];
  let current = [];
  for (const point of points) {
    if (point.acwr === null) {
      if (current.length) segments.push(current);
      current = [];
    } else {
      current.push(point);
    }
  }
  if (current.length) segments.push(current);
  return segments.filter((segment) => segment.length > 1);
}

function Chart({ weeks }) {
  const [active, setActive] = useState(null);

  const lastWeek = weeks.length;
  const highest = Math.max(...weeks.map((w) => w.acwr ?? 0));
  // At least 2.0, so the Spiking band always shows; more if a value goes past it.
  const yMax = Math.max(2, Math.ceil((highest + 0.1) * 10) / 10);
  const x = (week) => PAD.left + (lastWeek === 1 ? PLOT_W / 2 : ((week - 1) / (lastWeek - 1)) * PLOT_W);
  const y = (acwr) => PAD.top + (1 - acwr / yMax) * PLOT_H;

  const points = weeks.map((w) => ({ ...w, x: x(w.week), y: w.acwr === null ? null : y(w.acwr) }));
  const activePoint = points.find((p) => p.week === active);

  return (
    <div className="acwr-trend-chart">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        role="group"
        aria-label="ACWR by week. Tab to a point for its week, snaps and ACWR."
      >
        {BANDS.map((band) => {
          const top = y(band.to ?? yMax);
          return (
            <g key={band.label} className={`acwr-trend-band risk-${band.tier}`} aria-hidden="true">
              <rect x={PAD.left} y={top} width={PLOT_W} height={y(band.from) - top} />
              {/* On the left, where the first three games never have a point. */}
              <text x={PAD.left + 6} y={top + 13}>
                {band.label}
              </text>
            </g>
          );
        })}

        <g className="acwr-trend-axis" aria-hidden="true">
          {Y_TICKS.map((tick) => (
            <text key={tick} x={PAD.left - 6} y={y(tick) + 4} textAnchor="end">
              {tick.toFixed(1)}
            </text>
          ))}
          <line className="acwr-trend-baseline" x1={PAD.left} x2={PAD.left + PLOT_W} y1={y(1)} y2={y(1)} />
          {points.map((p) => (
            <text key={p.week} x={p.x} y={HEIGHT - 10} textAnchor="middle">
              {p.snaps === null ? "Bye" : `W${p.week}`}
            </text>
          ))}
        </g>

        {lineSegments(points).map((segment) => (
          <polyline
            key={segment[0].week}
            className="acwr-trend-line"
            points={segment.map((p) => `${p.x},${p.y}`).join(" ")}
            aria-hidden="true"
          />
        ))}

        {points
          .filter((p) => p.acwr !== null)
          .map((p) => (
            <circle
              key={p.week}
              className={`acwr-trend-point risk-${riskLevel(p.acwr).tier}`}
              cx={p.x}
              cy={p.y}
              r={active === p.week ? 7 : 5}
              tabIndex={0}
              role="img"
              aria-label={describeWeek(p)}
              onMouseEnter={() => setActive(p.week)}
              onMouseLeave={() => setActive(null)}
              onFocus={() => setActive(p.week)}
              onBlur={() => setActive(null)}
            />
          ))}
      </svg>

      {activePoint && (
        <div
          className="acwr-trend-tooltip"
          aria-hidden="true"
          style={{
            left: `${(activePoint.x / WIDTH) * 100}%`,
            top: `${(activePoint.y / HEIGHT) * 100}%`,
            // Centered on the point mid-chart, and kept inside the chart at either end.
            "--shift": `${-(activePoint.x / WIDTH) * 100}%`,
          }}
        >
          <strong>Week {activePoint.week}</strong>
          <span>{activePoint.snaps} snaps</span>
          <span>ACWR {format(activePoint.acwr)}</span>
        </div>
      )}
    </div>
  );
}

// The same data as the chart, for screen readers.
function DataTable({ weeks }) {
  return (
    <table className="visually-hidden">
      <caption>ACWR by week</caption>
      <thead>
        <tr>
          <th scope="col">Week</th>
          <th scope="col">Snaps</th>
          <th scope="col">ACWR</th>
          <th scope="col">Risk</th>
        </tr>
      </thead>
      <tbody>
        {weeks.map((w) => (
          <tr key={w.week}>
            <th scope="row">{w.week}</th>
            <td>{w.snaps === null ? "Bye" : w.snaps}</td>
            <td>{format(w.acwr)}</td>
            <td>{w.snaps === null ? "Bye week" : riskLevel(w.acwr).label}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

// Week-by-week ACWR for the player page. `games` is GET /players/{id}/workload's response:
// null while loading; `error` is a message when the request failed.
function AcwrTrendChart({ games, error }) {
  const weeks = games ? weeklyAcwr(games) : [];
  const hasValues = weeks.some((w) => w.acwr !== null);
  const trend = acwrTrend(weeks);

  let body;
  if (error) {
    body = <p className="acwr-trend-message">Couldn't load workload history: {error}</p>;
  } else if (!games) {
    body = <p className="acwr-trend-message">Loading workload history…</p>;
  } else if (!hasValues) {
    body = <p className="acwr-trend-message">No trend data yet.</p>;
  } else {
    body = (
      <>
        <Chart weeks={weeks} />
        {trend && (
          <p className="acwr-trend-summary">
            Last 3 weeks: <strong className={`acwr-trend-word trend-${trend.toLowerCase()}`}>{trend}</strong>
          </p>
        )}
        <DataTable weeks={weeks} />
      </>
    );
  }

  return (
    <section className="acwr-trend" aria-labelledby="acwr-trend-title">
      <div className="acwr-trend-header">
        <h2 id="acwr-trend-title">ACWR trend</h2>
        <span className="acwr-trend-sample">Sample data</span>
      </div>
      {body}
    </section>
  );
}

export default AcwrTrendChart;
