/** O "o" de Spot é uma bola com formato de pino de localização: o lugar onde se joga. */
export default function Logo() {
  return (
    <span className="logo" role="img" aria-label="PlaySpot">
      <span aria-hidden="true">PlaySp</span>
      <svg className="logo-pino" viewBox="0 0 24 30" aria-hidden="true">
        <path
          fill="#d6f24a"
          d="M12 0a12 12 0 0 1 12 12c0 7.5-7.6 14-12 18C7.6 26 0 19.5 0 12A12 12 0 0 1 12 0Z"
        />
        <path
          fill="none"
          stroke="#12284c"
          strokeWidth="1.9"
          transform="rotate(-32 12 12)"
          d="M4.2 2.9C8.8 7 8.8 17 4.2 21.1M19.8 2.9C15.2 7 15.2 17 19.8 21.1"
        />
      </svg>
      <span aria-hidden="true">t</span>
    </span>
  )
}
