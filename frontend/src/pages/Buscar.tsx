import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { LocateFixed, MapPin, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/client'
import type { Esporte, ResultadoBusca } from '../api/tipos'
import MapaResultados from '../components/MapaResultados'
import type { PontoDoMapa } from '../components/MapaResultados'
import { dataMaxima, formatarPreco, hoje } from '../formatos'

/** A busca ordena do mais próximo para o mais distante, então o raio pode ser amplo. */
const RAIO_KM = '100'

type Origem = { cidade: string } | { latitude: number; longitude: number }

interface Resposta {
  chave: string
  lista?: ResultadoBusca[]
  erro?: string
}

export default function Buscar() {
  const [esportes, setEsportes] = useState<Esporte[]>([])
  const [esporteId, setEsporteId] = useState('')
  const [data, setData] = useState(hoje)
  const [cidade, setCidade] = useState('')
  const [origem, setOrigem] = useState<Origem | null>(null)
  const [resposta, setResposta] = useState<Resposta | null>(null)
  const [localizando, setLocalizando] = useState(() => 'geolocation' in navigator)
  const [erroLocal, setErroLocal] = useState<string | null>(null)

  const pedirPosicao = useCallback((avisarSeFalhar: boolean) => {
    navigator.geolocation.getCurrentPosition(
      (posicao) => {
        setLocalizando(false)
        setErroLocal(null)
        setCidade('')
        // cerca de 100 metros de precisão: basta para a busca e evita enviar a posição exata
        setOrigem({
          latitude: Number(posicao.coords.latitude.toFixed(3)),
          longitude: Number(posicao.coords.longitude.toFixed(3)),
        })
      },
      () => {
        setLocalizando(false)
        if (avisarSeFalhar) setErroLocal('Não foi possível obter sua localização. Busque pela cidade.')
      },
      { timeout: 10000 },
    )
  }, [])

  useEffect(() => {
    api<Esporte[]>('/api/esportes').then(setEsportes).catch(() => setEsportes([]))
    if ('geolocation' in navigator) pedirPosicao(false)
  }, [pedirPosicao])

  function usarLocalizacao() {
    if (!('geolocation' in navigator)) {
      setErroLocal('Seu navegador não informa a localização. Busque pela cidade.')
      return
    }
    setErroLocal(null)
    setLocalizando(true)
    pedirPosicao(true)
  }

  const parametros = useMemo(() => {
    if (!origem) return null
    const busca = new URLSearchParams({ data })
    if (esporteId) busca.set('esporteId', esporteId)
    if ('cidade' in origem) {
      busca.set('cidade', origem.cidade)
    } else {
      busca.set('latitude', String(origem.latitude))
      busca.set('longitude', String(origem.longitude))
      busca.set('raioKm', RAIO_KM)
    }
    return busca.toString()
  }, [origem, esporteId, data])

  useEffect(() => {
    if (!parametros) return
    let ignorar = false
    api<ResultadoBusca[]>(`/api/busca?${parametros}`)
      .then((lista) => {
        if (!ignorar) setResposta({ chave: parametros, lista })
      })
      .catch((e) => {
        if (!ignorar) setResposta({ chave: parametros, erro: mensagemDeErro(e) })
      })
    return () => {
      ignorar = true
    }
  }, [parametros])

  const atual = resposta?.chave === parametros ? resposta : null
  const resultados = atual?.lista
  const buscando = parametros !== null && atual === null

  const pontos = useMemo<PontoDoMapa[]>(
    () =>
      (resultados ?? []).flatMap((r) =>
        r.latitude !== null && r.longitude !== null
          ? [{ id: r.id, nome: r.nome, latitude: r.latitude, longitude: r.longitude }]
          : [],
      ),
    [resultados],
  )

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErroLocal(null)
    setOrigem({ cidade })
  }

  const erro = erroLocal ?? atual?.erro
  const titulo = origem && 'cidade' in origem ? `Quadras em ${origem.cidade}` : 'Quadras perto de você'

  return (
    <main className="pagina">
      <section className="heroi">
        <h1>Encontre onde jogar</h1>
        <form className="busca" onSubmit={aoEnviar}>
          <label className="busca-cidade">
            Cidade
            <input
              value={cidade}
              onChange={(e) => setCidade(e.target.value)}
              maxLength={100}
              placeholder="Digite sua cidade"
              required
            />
          </label>
          <label>
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
          <label>
            Data
            <input
              type="date"
              value={data}
              min={hoje()}
              max={dataMaxima()}
              onChange={(e) => e.target.value && setData(e.target.value)}
            />
          </label>
          <div className="busca-acoes">
            <button
              type="button"
              className="secundario botao-icone"
              aria-label="Usar minha localização"
              title="Usar minha localização"
              onClick={usarLocalizacao}
              disabled={localizando}
            >
              <LocateFixed aria-hidden="true" />
            </button>
            <button type="submit">
              <Search aria-hidden="true" />
              Buscar
            </button>
          </div>
        </form>
      </section>

      {erro && <p className="erro resultado-vazio" role="alert">{erro}</p>}

      {!origem && !localizando && !erro && (
        <p className="estado-vazio">Digite sua cidade para ver as quadras disponíveis.</p>
      )}
      {(localizando || buscando) && !resultados && <p className="carregando">Procurando quadras…</p>}

      {resultados?.length === 0 && (
        <p className="estado-vazio">Nenhum horário livre nesta data. Tente outro dia ou outro esporte.</p>
      )}

      {resultados && resultados.length > 0 && (
        <section className="resultados" aria-label={titulo}>
          <div>
            <h2>{titulo}</h2>
            <ul>
              {resultados.map((resultado) => (
                <li key={resultado.id}>
                  <Link className="card-local" to={`/empresas/${resultado.id}?data=${data}`}>
                    {resultado.fotoId !== null ? (
                      <img src={`/api/fotos/${resultado.fotoId}`} alt="" />
                    ) : (
                      <div className="sem-foto" aria-hidden="true" />
                    )}
                    <div className="card-local-texto">
                      <h3>{resultado.nome}</h3>
                      <p>
                        <MapPin aria-hidden="true" />
                        {resultado.distanciaKm !== null
                          ? `${resultado.distanciaKm.toFixed(1).replace('.', ',')} km`
                          : `${resultado.cidade}/${resultado.uf}`}
                      </p>
                      <p>{resultado.esportes.join(' · ')}</p>
                    </div>
                    <div className="card-local-preco">
                      <small>a partir de</small>
                      <strong>{formatarPreco(resultado.precoAPartirDe)}</strong>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
          {pontos.length > 0 && (
            <div className="resultados-mapa">
              <MapaResultados pontos={pontos} data={data} />
            </div>
          )}
        </section>
      )}
    </main>
  )
}
