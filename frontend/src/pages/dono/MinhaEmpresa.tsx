import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, api, mensagemDeErro } from '../../api/client'
import { localizar } from '../../api/geocodificacao'
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
  const [aprovada, setAprovada] = useState<boolean | null>(null)
  const [avisoMapa, setAvisoMapa] = useState<string | null>(null)
  const [centroMapa, setCentroMapa] = useState<{ latitude: number; longitude: number } | null>(null)

  useEffect(() => {
    let ignorar = false
    api<Empresa>('/api/dono/empresa')
      .then((empresa) => {
        if (ignorar) return
        setAprovada(empresa.aprovada)
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
      const local = await localizar(dados.endereco, dados.cidade, dados.uf)
      if (!local) {
        setAvisoMapa('Não encontramos este endereço. Confira a cidade e a UF, ou clique no mapa para marcar o local.')
      } else if (local.exato) {
        marcar(local.latitude, local.longitude)
        setAvisoMapa('Confira o ponto no mapa. Se estiver errado, clique no local certo.')
      } else {
        // só achou o bairro ou a cidade: mostra a região, mas quem marca o ponto é o dono
        setCentroMapa({ latitude: local.latitude, longitude: local.longitude })
        setAvisoMapa('Não achamos a rua no mapa. Mostramos a região: clique no local exato da empresa.')
      }
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
      const empresa = await api<Empresa>('/api/dono/empresa', {
        method: 'PUT',
        corpo: {
          ...dados,
          documento: dados.documento.trim() || null,
          telefone: dados.telefone.trim() || null,
        },
      })
      setAprovada(empresa.aprovada)
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
      {aprovada === false && (
        <p className="aviso" role="status">
          Aguardando aprovação. Sua empresa ainda não aparece na busca, mas você já pode configurar quadras e horários.
        </p>
      )}
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
          <MapaSeletor latitude={dados.latitude} longitude={dados.longitude} centro={centroMapa} aoMudar={marcar} />
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
