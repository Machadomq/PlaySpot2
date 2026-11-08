import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, api, mensagemDeErro } from '../../api/client'
import type { Empresa } from '../../api/tipos'
import MapaSeletor from '../../components/MapaSeletor'

const VAZIA = {
  nome: '',
  documento: '',
  telefone: '',
  endereco: '',
  cidade: '',
  uf: '',
  prazoCancelamentoHoras: 24,
  latitude: null as number | null,
  longitude: null as number | null,
}

export default function MinhaEmpresa() {
  const [dados, setDados] = useState(VAZIA)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [salva, setSalva] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [localizando, setLocalizando] = useState(false)
  const [avisoMapa, setAvisoMapa] = useState<string | null>(null)

  useEffect(() => {
    let ignorar = false
    api<Empresa>('/api/dono/empresa')
      .then((empresa) => {
        if (ignorar) return
        setDados({
          ...empresa,
          documento: empresa.documento ?? '',
          telefone: empresa.telefone ?? '',
        })
      })
      .catch((e) => {
        // 404 é o caso normal de quem ainda não cadastrou a empresa
        if (!ignorar && !(e instanceof ApiError && e.status === 404)) setErro(mensagemDeErro(e))
      })
      .finally(() => {
        if (!ignorar) setCarregando(false)
      })
    return () => {
      ignorar = true
    }
  }, [])

  function alterar<C extends keyof typeof VAZIA>(campo: C, valor: (typeof VAZIA)[C]) {
    setDados((atual) => ({ ...atual, [campo]: valor }))
    setSalva(false)
  }

  function marcar(latitude: number, longitude: number) {
    setDados((atual) => ({ ...atual, latitude, longitude }))
    setSalva(false)
    setAvisoMapa(null)
  }

  async function localizarEndereco() {
    if (!dados.endereco.trim() || !dados.cidade.trim()) {
      setAvisoMapa('Preencha o endereço e a cidade primeiro.')
      return
    }
    setAvisoMapa(null)
    setLocalizando(true)
    try {
      const consulta = new URLSearchParams({
        format: 'json',
        limit: '1',
        countrycodes: 'br',
        q: `${dados.endereco}, ${dados.cidade}, ${dados.uf}`,
      })
      const resposta = await fetch(`https://nominatim.openstreetmap.org/search?${consulta}`)
      const lugares = (await resposta.json()) as { lat: string; lon: string }[]
      if (lugares.length === 0) {
        setAvisoMapa('Endereço não encontrado. Clique no mapa para marcar o local.')
        return
      }
      marcar(Number(Number(lugares[0].lat).toFixed(6)), Number(Number(lugares[0].lon).toFixed(6)))
      setAvisoMapa('Confira o ponto no mapa. Se estiver errado, clique no local certo.')
    } catch {
      setAvisoMapa('Não foi possível localizar agora. Clique no mapa para marcar o local.')
    } finally {
      setLocalizando(false)
    }
  }

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setSalva(false)
    if (dados.latitude === null) {
      setErro('Marque a localização da empresa no mapa.')
      return
    }
    setEnviando(true)
    try {
      await api<Empresa>('/api/dono/empresa', {
        method: 'PUT',
        corpo: {
          ...dados,
          documento: dados.documento.trim() || null,
          telefone: dados.telefone.trim() || null,
        },
      })
      setSalva(true)
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  if (carregando) return <p className="carregando">Carregando…</p>

  return (
    <main className="cartao largo">
      <h1>Minha empresa</h1>
      <form onSubmit={aoEnviar}>
        <label>
          Nome
          <input value={dados.nome} onChange={(e) => alterar('nome', e.target.value)} maxLength={120} required />
        </label>
        <label>
          Endereço
          <input
            value={dados.endereco}
            onChange={(e) => alterar('endereco', e.target.value)}
            maxLength={200}
            required
          />
        </label>
        <div className="linha">
          <label className="cresce">
            Cidade
            <input
              value={dados.cidade}
              onChange={(e) => alterar('cidade', e.target.value)}
              maxLength={100}
              required
            />
          </label>
          <label className="uf">
            UF
            <input
              value={dados.uf}
              onChange={(e) => alterar('uf', e.target.value.toUpperCase())}
              maxLength={2}
              minLength={2}
              required
            />
          </label>
        </div>
        <div className="campo-mapa">
          <span>Localização no mapa</span>
          <MapaSeletor latitude={dados.latitude} longitude={dados.longitude} aoMudar={marcar} />
          <button type="button" className="secundario" onClick={localizarEndereco} disabled={localizando}>
            {localizando ? 'Localizando…' : 'Localizar pelo endereço'}
          </button>
          {avisoMapa && <small>{avisoMapa}</small>}
        </div>
        <label>
          Telefone (opcional)
          <input
            type="tel"
            value={dados.telefone}
            onChange={(e) => alterar('telefone', e.target.value)}
            maxLength={20}
          />
        </label>
        <label>
          CNPJ ou CPF (opcional)
          <input
            value={dados.documento}
            onChange={(e) => alterar('documento', e.target.value)}
            maxLength={20}
          />
        </label>
        <label>
          Cancelamento pelo jogador até quantas horas antes do horário?
          <input
            type="number"
            min={0}
            max={720}
            value={dados.prazoCancelamentoHoras}
            onChange={(e) => alterar('prazoCancelamentoHoras', e.target.valueAsNumber)}
            required
          />
        </label>
        {erro && <p className="erro" role="alert">{erro}</p>}
        {salva && <p className="sucesso" role="status">Empresa salva.</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : 'Salvar'}
        </button>
      </form>
    </main>
  )
}
