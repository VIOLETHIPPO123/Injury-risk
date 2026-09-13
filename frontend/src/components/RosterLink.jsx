import './RosterLink.css'

function RosterLink({ onClick, children }) {
  return (
    <button type="button" className="roster-link" onClick={onClick}>
      {children}
    </button>
  )
}

export default RosterLink
