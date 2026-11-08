/** Desenho decorativo de uma quadra vista de cima, com a bola quicando. */
export default function CenaQuadra() {
  return (
    <div className="cena-quadra" aria-hidden="true">
      <svg viewBox="0 0 600 340" preserveAspectRatio="xMidYMid slice" fill="none" stroke="currentColor" strokeWidth="4">
        <rect x="40" y="30" width="520" height="280" rx="6" />
        <line x1="300" y1="30" x2="300" y2="310" />
        <circle cx="300" cy="170" r="52" />
        <rect x="40" y="95" width="82" height="150" />
        <rect x="478" y="95" width="82" height="150" />
      </svg>
      <span className="bola" />
    </div>
  )
}
