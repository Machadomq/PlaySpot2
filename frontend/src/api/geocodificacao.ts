const UFS: Record<string, string> = {
  AC: 'Acre', AL: 'Alagoas', AP: 'Amapá', AM: 'Amazonas', BA: 'Bahia', CE: 'Ceará', DF: 'Distrito Federal',
  ES: 'Espírito Santo', GO: 'Goiás', MA: 'Maranhão', MT: 'Mato Grosso', MS: 'Mato Grosso do Sul',
  MG: 'Minas Gerais', PA: 'Pará', PB: 'Paraíba', PR: 'Paraná', PE: 'Pernambuco', PI: 'Piauí',
  RJ: 'Rio de Janeiro', RN: 'Rio Grande do Norte', RS: 'Rio Grande do Sul', RO: 'Rondônia', RR: 'Roraima',
  SC: 'Santa Catarina', SP: 'São Paulo', SE: 'Sergipe', TO: 'Tocantins',
}

/** O serviço público do OpenStreetMap aceita no máximo uma consulta por segundo. */
const INTERVALO_MS = 1100

const SO_NUMERO = /^(?:n[º°.]?\s*)?(\d+[a-z]?)$/i
const NUMERO_NO_FIM = /\s+(?:n[º°.]?\s*)?(\d+[a-z]?)$/i

export interface Local {
  latitude: number
  longitude: number
  /** true: achou a rua. false: achou só o bairro ou a cidade, então não serve como posição da empresa. */
  exato: boolean
}

async function consultar(parametros: Record<string, string>): Promise<{ lat: string; lon: string } | null> {
  const consulta = new URLSearchParams({ format: 'json', limit: '1', countrycodes: 'br', ...parametros })
  const resposta = await fetch(`https://nominatim.openstreetmap.org/search?${consulta}`)
  if (!resposta.ok) throw new Error(`Falha na busca de endereço: ${resposta.status}`)
  const lugares = (await resposta.json()) as { lat: string; lon: string }[]
  return lugares[0] ?? null
}

/** Do mais preciso para o menos preciso: rua com número, rua, outras partes do endereço (bairro) e cidade. */
export async function localizar(endereco: string, cidade: string, uf: string): Promise<Local | null> {
  const estado = UFS[uf] ?? uf
  const partes = endereco.split(/[,;]|\s-\s/).map((p) => p.trim()).filter(Boolean)

  let numero = partes.map((p) => SO_NUMERO.exec(p)?.[1]).find(Boolean)
  const textos = partes.filter((p) => !SO_NUMERO.test(p))
  const primeira = textos[0] ?? ''
  numero ??= NUMERO_NO_FIM.exec(primeira)?.[1]
  const rua = primeira.replace(NUMERO_NO_FIM, '')

  const tentativas: { parametros: Record<string, string>; exato: boolean }[] = []
  if (rua && numero) tentativas.push({ parametros: { street: `${numero} ${rua}`, city: cidade, state: estado }, exato: true })
  if (rua) tentativas.push({ parametros: { street: rua, city: cidade, state: estado }, exato: true })
  for (const outra of textos.slice(1, 2)) {
    tentativas.push({ parametros: { q: `${outra}, ${cidade}, ${estado}` }, exato: false })
  }
  tentativas.push({ parametros: { city: cidade, state: estado }, exato: false })

  for (const [indice, { parametros, exato }] of tentativas.entries()) {
    if (indice > 0) await new Promise((r) => setTimeout(r, INTERVALO_MS))
    const lugar = await consultar(parametros)
    if (lugar) {
      return { latitude: Number(Number(lugar.lat).toFixed(6)), longitude: Number(Number(lugar.lon).toFixed(6)), exato }
    }
  }
  return null
}
