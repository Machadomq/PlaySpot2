import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/client'
import type { EmpresaPublica as Empresa, HorarioDisponivel, Quadra, Reserva } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import { dataMaxima, formatarPreco, hoje } from '../formatos'

type HorariosPorQuadra = Record<number, HorarioDisponivel[]>

interface Escolha {
  quadra: Quadra
  horario: HorarioDisponivel
}

const horaCurta = (hora: string) => hora.slice(0, 5)

export default function EmpresaPublica() {
  const { id } = useParams()
  const [parametros, setParametros] = useSearchParams()
  const data = parametros.get('data') ?? hoje()
  const { usuario } = useAuth()
  const navegar = useNavigate()
  const local = useLocation()

  const [empresa, setEmpresa] = useState<Empresa | null>(null)
  const [horarios, setHorarios] = useState<HorariosPorQuadra>({})
  const [versao, setVersao] = useState(0)
  const [escolha, setEscolha] = useState<Escolha | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let ignorar = false
    api<Empresa>(`/api/empresas/${id}`)
      .then(async (dados) => {
        const listas = await Promise.all(
          dados.quadras.map((q) => api<HorarioDisponivel[]>(`/api/quadras/${q.id}/horarios?data=${data}`)),
        )
        if (ignorar) return
        setEmpresa(dados)
        setHorarios(Object.fromEntries(dados.quadras.map((q, i) => [q.id, listas[i]])))
      })
      .catch((e) => {
        if (!ignorar) setErro(mensagemDeErro(e))
      })
    return () => {
      ignorar = true
    }
  }, [id, data, versao])

  function trocarData(novaData: string) {
    if (!novaData) return
    setEscolha(null)
    setErro(null)
    setParametros({ data: novaData }, { replace: true })
  }

  async function confirmar() {
    if (!escolha) return
    if (!usuario) {
      navegar('/login', { state: { de: local.pathname + local.search } })
      return
    }
    setErro(null)
    setEnviando(true)
    try {
      await api<Reserva>('/api/reservas', {
        method: 'POST',
        corpo: { quadraId: escolha.quadra.id, inicio: escolha.horario.inicio },
      })
      navegar('/reservas')
    } catch (e) {
      setErro(mensagemDeErro(e))
      setEscolha(null)
      setVersao((v) => v + 1)
    } finally {
      setEnviando(false)
    }
  }

  if (!empresa) {
    return erro ? <p className="erro central" role="alert">{erro}</p> : <p className="carregando">Carregando…</p>
  }

  const podeReservar = !usuario || usuario.papel === 'JOGADOR'

  return (
    <main className="cartao largo">
      <h1>{empresa.nome}</h1>
      <p className="vazio">
        {empresa.endereco}, {empresa.cidade}/{empresa.uf}
        {empresa.telefone && ` · ${empresa.telefone}`}
      </p>
      <label className="campo-data">
        Data
        <input type="date" value={data} min={hoje()} max={dataMaxima()} onChange={(e) => trocarData(e.target.value)} />
      </label>
      {erro && <p className="erro" role="alert">{erro}</p>}

      {empresa.quadras.map((quadra) => (
        <section className="secao-fotos" key={quadra.id}>
          <h2>{quadra.nome}</h2>
          <p className="vazio">{quadra.esportes.map((e) => e.nome).join(', ')}</p>
          {quadra.descricao && <p>{quadra.descricao}</p>}
          {quadra.fotoIds.length > 0 && (
            <div className="fotos">
              {quadra.fotoIds.map((fotoId) => (
                <figure key={fotoId}>
                  <img src={`/api/fotos/${fotoId}`} alt={`Foto de ${quadra.nome}`} />
                </figure>
              ))}
            </div>
          )}
          {horarios[quadra.id]?.length === 0 && <p className="vazio">Sem horários nesta data.</p>}
          <ul className="horarios">
            {horarios[quadra.id]?.map((horario) => (
              <li key={horario.inicio}>
                <button
                  type="button"
                  className={`horario${escolha?.horario.inicio === horario.inicio && escolha.quadra.id === quadra.id ? ' selecionado' : ''}`}
                  disabled={!horario.livre}
                  onClick={() => setEscolha({ quadra, horario })}
                >
                  <strong>
                    {horaCurta(horario.horaInicio)} às {horaCurta(horario.horaFim)}
                  </strong>
                  <span>{horario.livre ? formatarPreco(horario.preco) : 'Indisponível'}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}

      {escolha && (
        <aside className="confirmacao">
          <p>
            <strong>{escolha.quadra.nome}</strong>, {horaCurta(escolha.horario.horaInicio)} às{' '}
            {horaCurta(escolha.horario.horaFim)}: <strong>{formatarPreco(escolha.horario.preco)}</strong>
          </p>
          <small>
            Pagamento no local. Cancelamento pelo app até {empresa.prazoCancelamentoHoras}h antes do horário.
          </small>
          {podeReservar ? (
            <button type="button" onClick={confirmar} disabled={enviando}>
              {enviando ? 'Reservando…' : usuario ? 'Confirmar reserva' : 'Entrar para reservar'}
            </button>
          ) : (
            <p className="erro">Reservas são feitas com uma conta de jogador.</p>
          )}
        </aside>
      )}
    </main>
  )
}
