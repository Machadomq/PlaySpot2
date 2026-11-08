import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { LocateFixed, MapPin, Search } from 'lucide-react'
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
    <main className="pagina">
      <section className="heroi">
        <h1>Encontre onde jogar</h1>
        <p>Compare quadras e preços perto de você e reserve o horário.</p>
      </section>

      <form className="busca" onSubmit={aoEnviar}>
        <fieldset className="chips">
          <legend>Esporte</legend>
          <button type="button" className="chip" aria-pressed={esporteId === ''} onClick={() => setEsporteId('')}>
            Todos
          </button>
          {esportes.map((esporte) => (
            <button
              key={esporte.id}
              type="button"
              className="chip"
              aria-pressed={esporteId === String(esporte.id)}
              onClick={() => setEsporteId(String(esporte.id))}
            >
              {esporte.nome}
            </button>
          ))}
        </fieldset>

        <div className="busca-campos">
          <label>
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
          <label>
            Distância
            <select value={raioKm} onChange={(e) => setRaioKm(e.target.value)}>
              <option value="10">10 km</option>
              <option value="30">30 km</option>
              <option value="50">50 km</option>
              <option value="100">100 km</option>
            </select>
          </label>
        </div>

        <button type="button" className="destaque" onClick={usarLocalizacao} disabled={localizando || buscando}>
          <LocateFixed aria-hidden="true" />
          {localizando ? 'Localizando…' : 'Quadras perto de mim'}
        </button>

        <div className="ou"><span>ou</span></div>

        <div className="busca-cidade">
          <label>
            Busque pela cidade
            <input
              value={cidade}
              onChange={(e) => setCidade(e.target.value)}
              maxLength={100}
              placeholder="Ex.: Jaraguá do Sul"
              required
            />
          </label>
          <button type="submit" disabled={buscando}>
            <Search aria-hidden="true" />
            {buscando ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
        {erro && <p className="erro" role="alert">{erro}</p>}
      </form>

      {resultados === null && (
        <ol className="passos">
          <li><strong>Escolha</strong> o esporte e o dia.</li>
          <li><strong>Compare</strong> locais, preços e horários livres.</li>
          <li><strong>Reserve</strong> na hora e pague no local.</li>
        </ol>
      )}

      {resultados?.length === 0 && (
        <p className="estado-vazio">Nenhum horário livre com esses filtros. Tente outro dia ou aumente a distância.</p>
      )}

      {resultados && resultados.length > 0 && (
        <section className="resultados" aria-label="Resultados da busca">
          <ul>
            {resultados.map((resultado) => (
              <li key={resultado.id}>
                <Link className="card-local" to={`/empresas/${resultado.id}?data=${dataBuscada}`}>
                  {resultado.fotoId !== null ? (
                    <img src={`/api/fotos/${resultado.fotoId}`} alt="" />
                  ) : (
                    <div className="sem-foto" aria-hidden="true" />
                  )}
                  <div className="card-local-texto">
                    <h2>{resultado.nome}</h2>
                    <p>
                      <MapPin aria-hidden="true" />
                      {resultado.cidade}/{resultado.uf}
                      {resultado.distanciaKm !== null && ` · ${resultado.distanciaKm.toFixed(1).replace('.', ',')} km`}
                    </p>
                    <p className="card-esportes">
                      {resultado.esportes.join(' · ')}
                    </p>
                  </div>
                  <div className="card-local-preco">
                    <small>a partir de</small>
                    <strong>{formatarPreco(resultado.precoAPartirDe)}</strong>
                    <span className="selo-livres">
                      {resultado.horariosLivres} {resultado.horariosLivres === 1 ? 'horário' : 'horários'}
                    </span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          {pontos.length > 0 && (
            <div className="resultados-mapa">
              <MapaResultados pontos={pontos} data={dataBuscada} />
            </div>
          )}
        </section>
      )}
    </main>
  )
}
