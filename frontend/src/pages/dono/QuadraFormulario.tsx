import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../../api/client'
import type { DadosQuadra, Esporte, Quadra } from '../../api/tipos'

const MAXIMO_DE_FOTOS = 5
const TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024

export default function QuadraFormulario() {
  const { id } = useParams()
  const quadraId = id ? Number(id) : null
  const navegar = useNavigate()

  const [esportes, setEsportes] = useState<Esporte[]>([])
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [duracao, setDuracao] = useState(60)
  const [ativa, setAtiva] = useState(true)
  const [selecionados, setSelecionados] = useState<number[]>([])
  const [fotoIds, setFotoIds] = useState<number[]>([])
  const [carregando, setCarregando] = useState(true)
  const [naoEncontrada, setNaoEncontrada] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [salva, setSalva] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [enviandoFoto, setEnviandoFoto] = useState(false)

  useEffect(() => {
    let ignorar = false
    Promise.all([
      api<Esporte[]>('/api/esportes'),
      quadraId !== null ? api<Quadra[]>('/api/dono/quadras') : Promise.resolve([]),
    ])
      .then(([listaEsportes, quadras]) => {
        if (ignorar) return
        setEsportes(listaEsportes)
        if (quadraId === null) return
        const quadra = quadras.find((q) => q.id === quadraId)
        if (!quadra) {
          setNaoEncontrada(true)
          return
        }
        setNome(quadra.nome)
        setDescricao(quadra.descricao ?? '')
        setDuracao(quadra.duracaoSlotMin)
        setAtiva(quadra.ativa)
        setSelecionados(quadra.esportes.map((e) => e.id))
        setFotoIds(quadra.fotoIds)
      })
      .catch((e) => {
        if (!ignorar) setErro(mensagemDeErro(e))
      })
      .finally(() => {
        if (!ignorar) setCarregando(false)
      })
    return () => {
      ignorar = true
    }
  }, [quadraId])

  function alternarEsporte(esporteId: number) {
    setSelecionados((atual) =>
      atual.includes(esporteId) ? atual.filter((e) => e !== esporteId) : [...atual, esporteId],
    )
  }

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setSalva(false)
    if (selecionados.length === 0) {
      setErro('Escolha pelo menos um esporte.')
      return
    }
    const corpo: DadosQuadra = {
      nome,
      descricao: descricao.trim() || null,
      duracaoSlotMin: duracao,
      ativa,
      esporteIds: selecionados,
    }
    setEnviando(true)
    try {
      if (quadraId === null) {
        const criada = await api<Quadra>('/api/dono/quadras', { method: 'POST', corpo })
        navegar(`/dono/quadras/${criada.id}`, { replace: true })
      } else {
        await api<Quadra>(`/api/dono/quadras/${quadraId}`, { method: 'PUT', corpo })
        setSalva(true)
      }
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  async function aoEscolherFoto(evento: ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0]
    evento.target.value = ''
    if (!arquivo || quadraId === null) return
    setErro(null)
    if (arquivo.size > TAMANHO_MAXIMO_BYTES) {
      setErro('O arquivo pode ter no máximo 5 MB.')
      return
    }
    const formulario = new FormData()
    formulario.append('arquivo', arquivo)
    setEnviandoFoto(true)
    try {
      const quadra = await api<Quadra>(`/api/dono/quadras/${quadraId}/fotos`, {
        method: 'POST',
        corpo: formulario,
      })
      setFotoIds(quadra.fotoIds)
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviandoFoto(false)
    }
  }

  async function removerFoto(fotoId: number) {
    setErro(null)
    try {
      const quadra = await api<Quadra>(`/api/dono/quadras/${quadraId}/fotos/${fotoId}`, { method: 'DELETE' })
      setFotoIds(quadra.fotoIds)
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  if (carregando) return <p className="carregando">Carregando…</p>
  if (naoEncontrada) {
    return (
      <main className="cartao largo">
        <h1>Quadra não encontrada</h1>
        <Link to="/dono/quadras">Voltar para as quadras</Link>
      </main>
    )
  }

  return (
    <main className="cartao largo">
      <h1>{quadraId === null ? 'Nova quadra' : 'Editar quadra'}</h1>
      <form onSubmit={aoEnviar}>
        <label>
          Nome
          <input value={nome} onChange={(e) => setNome(e.target.value)} maxLength={80} required />
        </label>
        <label>
          Descrição (opcional)
          <textarea
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
            maxLength={1000}
            rows={3}
          />
        </label>
        <label>
          Duração de cada horário
          <select value={duracao} onChange={(e) => setDuracao(Number(e.target.value))}>
            <option value={30}>30 minutos</option>
            <option value={60}>1 hora</option>
            <option value={90}>1 hora e 30 minutos</option>
            <option value={120}>2 horas</option>
          </select>
        </label>
        <fieldset>
          <legend>Esportes</legend>
          <div className="checks">
            {esportes.map((esporte) => (
              <label className="opcao" key={esporte.id}>
                <input
                  type="checkbox"
                  checked={selecionados.includes(esporte.id)}
                  onChange={() => alternarEsporte(esporte.id)}
                />
                {esporte.nome}
              </label>
            ))}
          </div>
        </fieldset>
        <label className="opcao">
          <input type="checkbox" checked={ativa} onChange={(e) => setAtiva(e.target.checked)} />
          Quadra ativa
        </label>
        {erro && <p className="erro" role="alert">{erro}</p>}
        {salva && <p className="sucesso" role="status">Quadra salva.</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : quadraId === null ? 'Criar e adicionar fotos' : 'Salvar'}
        </button>
      </form>

      {quadraId !== null && (
        <section className="secao-fotos">
          <h2>
            Fotos ({fotoIds.length}/{MAXIMO_DE_FOTOS})
          </h2>
          <div className="fotos">
            {fotoIds.map((fotoId) => (
              <figure key={fotoId}>
                <img src={`/api/fotos/${fotoId}`} alt="Foto da quadra" />
                <button type="button" className="secundario" onClick={() => removerFoto(fotoId)}>
                  Remover
                </button>
              </figure>
            ))}
          </div>
          {fotoIds.length < MAXIMO_DE_FOTOS && (
            <label>
              {enviandoFoto ? 'Enviando…' : 'Adicionar foto (JPG, PNG ou WebP, até 5 MB)'}
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={aoEscolherFoto}
                disabled={enviandoFoto}
              />
            </label>
          )}
        </section>
      )}

      <p className="rodape">
        <Link to="/dono/quadras">Voltar para as quadras</Link>
      </p>
    </main>
  )
}
