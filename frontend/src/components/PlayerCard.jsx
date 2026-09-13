import './PlayerCard.css'

// ACWR (Acute:Chronic Workload Ratio) compares the most recent game's workload
// against the average of the last four. Roughly 1.0 means "business as usual";
// a big spike is the classic soft-tissue injury warning sign.
function calculateAcwr(snapsLastGame, snapsLast4Games) {
  const chronic = snapsLast4Games / 4
  if (!chronic) return null
  return snapsLastGame / chronic
}

// tier drives both the text color and the card's background tint —
// under-training and slightly-elevated share "medium" because the risk
// doc treats both as cautionary, just from opposite directions.
function riskLevel(acwr) {
  if (acwr === null) return { label: 'Not enough data', tier: 'unknown' }
  if (acwr < 0.8) return { label: 'Ramping up', tier: 'medium' }
  if (acwr <= 1.3) return { label: 'Normal workload', tier: 'low' }
  if (acwr <= 1.5) return { label: 'Slightly elevated', tier: 'medium' }
  return { label: 'Spiking — high risk', tier: 'high' }
}

function PlayerCard({ name, team, snapsLastGame, snapsLast4Games }) {
  const acwr = calculateAcwr(snapsLastGame, snapsLast4Games)
  const risk = riskLevel(acwr)

  return (
    <article className={`player-card risk-${risk.tier}`}>
      <header className="player-card-header">
        <h2>{name}</h2>
        <span className="team">{team}</span>
      </header>

      <div className={`acwr risk-${risk.tier}`}>
        <span className="acwr-value">{acwr === null ? '—' : acwr.toFixed(2)}</span>
        <span className="acwr-label">{risk.label}</span>
      </div>

      <dl className="snaps">
        <div>
          <dt>Last game</dt>
          <dd>{snapsLastGame}</dd>
        </div>
        <div>
          <dt>Last 4 games</dt>
          <dd>{snapsLast4Games}</dd>
        </div>
      </dl>
    </article>
  )
}

export default PlayerCard
