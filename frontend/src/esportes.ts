const ICONES: [RegExp, string][] = [
  [/fute|society|soccer/i, '⚽'],
  [/futsal/i, '⚽'],
  [/basquete/i, '🏀'],
  [/v[oô]lei/i, '🏐'],
  [/t[eê]n+is|padel|pickle/i, '🎾'],
  [/handebol/i, '🤾'],
]

/** Esportes novos, cadastrados pelo administrador, recebem o ícone genérico. */
export function iconeDoEsporte(nome: string) {
  return ICONES.find(([padrao]) => padrao.test(nome))?.[1] ?? '🏅'
}
