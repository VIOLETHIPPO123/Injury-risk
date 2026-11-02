import { describeHistory, describeRepeat, repeatedInjuries } from "../utils/injuryPatterns";
import "./InjuryHistory.css";

// A player's past injuries, in the order given (the API sends them newest first), with a
// summary line above that warns about any body part injured more than once.
// `injuries` is null while loading; `error` is a message when the request failed.
function InjuryHistory({ injuries, error }) {
  const repeats = injuries ? repeatedInjuries(injuries) : [];

  let body;
  if (error) {
    body = <p className="injury-history-message">Couldn't load injury history: {error}</p>;
  } else if (!injuries) {
    body = <p className="injury-history-message">Loading injury history…</p>;
  } else if (injuries.length === 0) {
    body = <p className="injury-history-message">No recorded injuries.</p>;
  } else {
    body = (
      <>
        {repeats.length > 0 ? (
          <div className="injury-history-warning" role="note" aria-label="Repeat injuries">
            {repeats.map((repeat) => (
              <p key={repeat.bodyPart}>⚠ {describeRepeat(repeat)}</p>
            ))}
          </div>
        ) : (
          <p className="injury-history-summary">{describeHistory(injuries)}</p>
        )}
        <table className="injury-history-table">
          <thead>
            <tr>
              <th scope="col">Season</th>
              <th scope="col">Week</th>
              <th scope="col">Body part</th>
              <th scope="col">Type</th>
              <th scope="col">Games missed</th>
              <th scope="col">Status</th>
            </tr>
          </thead>
          <tbody>
            {injuries.map((injury, i) => (
              <tr key={i}>
                <td>{injury.season}</td>
                <td>{injury.week}</td>
                <td>{injury.bodyPart}</td>
                <td>{injury.type}</td>
                <td>{injury.gamesMissed}</td>
                <td>{injury.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </>
    );
  }

  return (
    <section className="injury-history" aria-labelledby="injury-history-title">
      <div className="injury-history-header">
        <h2 id="injury-history-title">Injury history</h2>
        <span className="injury-history-sample">Sample data</span>
      </div>
      {body}
    </section>
  );
}

export default InjuryHistory;
