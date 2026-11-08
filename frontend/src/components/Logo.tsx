/** O "o" de Spot é um pino de localização com o símbolo de play: o lugar onde se joga. */
export default function Logo() {
  return (
    <span className="logo" role="img" aria-label="PlaySpot">
      <span aria-hidden="true">PlaySp</span>
      <svg className="logo-pino" viewBox="0 0 24 30" aria-hidden="true">
        <path
          fill="#d6f24a"
          d="M12 0a12 12 0 0 1 12 12c0 7.5-7.6 14-12 18C7.6 26 0 19.5 0 12A12 12 0 0 1 12 0Z"
        />
        <path fill="#12284c" stroke="#12284c" strokeWidth="1.6" strokeLinejoin="round" d="M9.4 7.2v9.6l7.8-4.8Z" />
      </svg>
      <span aria-hidden="true">t</span>
    </span>
  )
}
