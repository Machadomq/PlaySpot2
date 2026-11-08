import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/client'
import type { Esporte, ResultadoBusca } from '../api/tipos'
import MapaResultados from '../components/MapaResultados'
import type { PontoDoMapa } from '../components/MapaResultados'
import { dataMaxima, formatarPreco, hoje } from '../formatos'

interface Coordenadas {
  latitude: number
  longitude: number
}

export default function Buscar() {
  const [esportes, setEsportes] = useState<Esporte[]>([])
  const [esporteId, setEsporteId] = useState('')
  const [data, setData] = useState(hoje)
  const [dataBuscada, setDataBuscada] = useState(data)
  const [cidade, setCidade] = useState('')
  const [raioKm, setRaioKm] = useState('30')
  const [resultados, setResultados] = useState<ResultadoBusca[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [buscando, setBuscando] = useState(false)
  const [localizando, setLocalizando] = useState(false)

  useEffect(() => {
    api<Esporte[]>('/api/esportes').then(setEsportes).catch((e) => setErro(mensagemDeErro(e)))
  }, [])

  const pontos = useMemo<PontoDoMapa[]>(
    () =>
      (resultados ?? []).flatMap((r) =>
        r.latitude !== null && r.longitude !== null
          ? [{ id: r.id, nome: r.nome, latitude: r.latitude, longitude: r.longitude }]
          : [],
      ),
    [resultados],
  )

  async function buscar(filtros: { cidade?: string; coordenadas?: Coordenadas }) {
    const parametros = new URLSearchParams({ data })
    if (esporteId) parametros.set('esporteId', esporteId)
    if (filtros.cidade) parametros.set('cidade', filtros.cidade)
    if (filtros.coordenadas) {
      parametros.set('latitude', String(filtros.coordenadas.latitude))
      parametros.set('longitude', String(filtros.coordenadas.longitude))
      parametros.set('raioKm', raioKm)
    }
    setErro(null)
    setBuscando(true)
    try {
      setResultados(await api<ResultadoBusca[]>(`/api/busca?${parametros}`))
      setDataBuscada(data)
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setBuscando(false)
    }
  }

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    void buscar({ cidade })
  }

  function usarLocalizacao() {
    if (!navigator.geolocation) {
      setErro('Seu navegador não informa a localização. Busque pela cidade.')
      return
    }
    setErro(null)
    setLocalizando(true)
    navigator.geolocation.getCurrentPosition(
      (posicao) => {
        setLocalizando(false)
        // cerca de 100 metros de precisão: basta para a busca e evita enviar a posição exata
        void buscar({
          coordenadas: {
            latitude: Number(posicao.coords.latitude.toFixed(3)),
            longitude: Number(posicao.coords.longitude.toFixed(3)),
          },
        })
      },
      () => {
        setLocalizando(false)
        setErro('Não foi possível obter sua localização. Busque pela cidade.')
      },
      { timeout: 10000 },
    )
  }

  return (
    <main className="cartao largo">
      <h1>Encontre onde jogar</h1>
      <form onSubmit={aoEnviar}>
        <div className="linha">
          <label className="cresce">
            Esporte
            <select value={esporteId} onChange={(e) => setEsporteId(e.target.value)}>
              <option value="">Todos</option>
              {esportes.map((esporte) => (
                <option key={esporte.id} value={esporte.id}>
                  {esporte.nome}
                </option>
              ))}
            </select>
          </label>
          <label className="cresce">
            Data
            <input
              type="date"
              value={data}
              min={hoje()}
              max={dataMaxima()}
              onChange={(e) => setData(e.target.value)}
              required
            />
          </label>
        </div>
        <div className="linha">
          <label className="cresce">
            Distância máxima
            <select value={raioKm} onChange={(e) => setRaioKm(e.target.value)}>
              <option value="10">10 km</option>
              <option value="30">30 km</option>
              <option value="50">50 km</option>
              <option value="100">100 km</option>
            </select>
          </label>
          <button type="button" className="secundario alinhado" onClick={usarLocalizacao} disabled={localizando || buscando}>
            {localizando ? 'Localizando…' : 'Perto de mim'}
          </button>
        </div>
        <div className="linha">
          <label className="cresce">
            Ou busque pela cidade
            <input value={cidade} onChange={(e) => setCidade(e.target.value)} maxLength={100} required />
          </label>
          <button type="submit" className="alinhado" disabled={buscando}>
            {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
        {erro && <p className="erro" role="alert">{erro}</p>}
      </form>

      {resultados?.length === 0 && <p className="vazio resultado-vazio">Nenhum horário livre encontrado com esses filtros.</p>}
      {resultados && resultados.length > 0 && (
        <>
          {pontos.length > 0 && <MapaResultados pontos={pontos} data={dataBuscada} />}
          <ul className="lista">
            {resultados.map((resultado) => (
              <li key={resultado.id}>
                {resultado.fotoId !== null ? (
                  <img src={`/api/fotos/${resultado.fotoId}`} alt="" />
                ) : (
                  <div className="sem-foto" aria-hidden="true" />
                )}
                <div className="cresce">
                  <Link to={`/empresas/${resultado.id}?data=${dataBuscada}`}>{resultado.nome}</Link>
                  <small>
                    {resultado.cidade}/{resultado.uf}
                    {resultado.distanciaKm !== null && ` · ${resultado.distanciaKm.toFixed(1).replace('.', ',')} km`}
                  </small>
                  <small>{resultado.esportes.join(', ')}</small>
                </div>
                <div className="preco">
                  <strong>a partir de {formatarPreco(resultado.precoAPartirDe)}</strong>
                  <small>
                    {resultado.horariosLivres} {resultado.horariosLivres === 1 ? 'horário livre' : 'horários livres'}
                  </small>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  )
}
