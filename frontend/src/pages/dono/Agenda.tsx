import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../../api/client'
import type { AgendaItem, HorarioDisponivel, Quadra } from '../../api/tipos'
import AcoesDaReserva from '../../components/AcoesDaReserva'
import { formatarDia, formatarPreco, hoje, segundaDaSemana, somarDias } from '../../formatos'

type Visao = 'dia' | 'semana'

interface Selecao {
  quadra: Quadra
  horario: HorarioDisponivel
}

const horaCurta = (hora: string) => hora.slice(0, 5)
const sobrepoe = (item: AgendaItem, horario: HorarioDisponivel) =>
  new Date(item.inicio) < new Date(horario.fim) && new Date(item.fim) > new Date(horario.inicio)

export default function Agenda() {
  const [visao, setVisao] = useState<Visao>('dia')
  const [data, setData] = useState(hoje)
  const [quadras, setQuadras] = useState<Quadra[]>([])
  const [itens, setItens] = useState<AgendaItem[]>([])
  const [grades, setGrades] = useState<Record<number, HorarioDisponivel[]>>({})
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [selecao, setSelecao] = useState<Selecao | null>(null)

  const inicioPeriodo = visao === 'dia' ? data : segundaDaSemana(data)
  const fimPeriodo = visao === 'dia' ? data : somarDias(inicioPeriodo, 6)

  const carregar = useCallback(async () => {
    try {
      const [listaQuadras, listaItens] = await Promise.all([
        api<Quadra[]>('/api/dono/quadras'),
        api<AgendaItem[]>(`/api/dono/agenda?de=${inicioPeriodo}&ate=${fimPeriodo}`),
      ])
      const ativas = listaQuadras.filter((q) => q.ativa)
      const listas = await Promise.all(
        ativas.map((q) => api<HorarioDisponivel[]>(`/api/dono/quadras/${q.id}/grade?data=${inicioPeriodo}`)),
      )
      setQuadras(ativas)
      setItens(listaItens)
      setGrades(Object.fromEntries(ativas.map((q, i) => [q.id, listas[i]])))
      setErro(null)
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setCarregando(false)
    }
  }, [inicioPeriodo, fimPeriodo])

  useEffect(() => {
    void carregar()
  }, [carregar])

  function mover(direcao: number) {
    setSelecao(null)
    setData((atual) => somarDias(atual, direcao * (visao === 'dia' ? 1 : 7)))
  }

  function aoLancar() {
    setSelecao(null)
    void carregar()
  }

  const dias = Array.from({ length: 7 }, (_, i) => somarDias(inicioPeriodo, i))

  return (
    <main className="cartao largo">
      <div className="cabecalho-secao">
        <h1>Agenda</h1>
        <div className="alternador" role="group" aria-label="Visualização">
          <button type="button" className={visao === 'dia' ? '' : 'secundario'} onClick={() => setVisao('dia')}>
            Dia
          </button>
          <button type="button" className={visao === 'semana' ? '' : 'secundario'} onClick={() => setVisao('semana')}>
            Semana
          </button>
        </div>
      </div>

      <div className="navegacao-data">
        <button type="button" className="secundario" aria-label="Período anterior" onClick={() => mover(-1)}>
          ‹
        </button>
        <input
          type="date"
          aria-label="Data"
          value={data}
          onChange={(e) => {
            if (e.target.value) {
              setSelecao(null)
              setData(e.target.value)
            }
          }}
        />
        <button type="button" className="secundario" aria-label="Próximo período" onClick={() => mover(1)}>
          ›
        </button>
      </div>

      {erro && <p className="erro" role="alert">{erro}</p>}
      {carregando && <p className="carregando">Carregando…</p>}

      {!carregando && quadras.length === 0 && (
        <p className="vazio">
          Nenhuma quadra ativa. <Link to="/dono/quadras">Cadastre suas quadras</Link>.
        </p>
      )}

      {visao === 'dia' &&
        quadras.map((quadra) => {
          const doDia = itens.filter((i) => i.quadraId === quadra.id)
          const grade = grades[quadra.id] ?? []
          const foraDaGrade = doDia.filter((i) => !grade.some((h) => sobrepoe(i, h)))
          return (
            <section className="secao-fotos" key={quadra.id}>
              <h2>{quadra.nome}</h2>
              {grade.length === 0 && foraDaGrade.length === 0 && (
                <p className="vazio">
                  Sem horários neste dia. <Link to={`/dono/quadras/${quadra.id}/horarios`}>Configurar</Link>
                </p>
              )}
              <ul className="agenda">
                {grade.map((horario) => {
                  const item = doDia.find((i) => sobrepoe(i, horario))
                  return (
                    <li key={horario.inicio}>
                      <span className="hora">
                        {horaCurta(horario.horaInicio)}
                        <small>{horaCurta(horario.horaFim)}</small>
                      </span>
                      {item ? (
                        <AcoesDaReserva item={item} aoMudar={() => void carregar()} />
                      ) : (
                        <button
                          type="button"
                          className="horario-vago"
                          onClick={() => setSelecao({ quadra, horario })}
                        >
                          Livre · {formatarPreco(horario.preco)}
                        </button>
                      )}
                    </li>
                  )
                })}
                {foraDaGrade.map((item) => (
                  <li key={item.id}>
                    <span className="hora">
                      {horaCurta(item.horaInicio)}
                      <small>{horaCurta(item.horaFim)}</small>
                    </span>
                    <AcoesDaReserva item={item} aoMudar={() => void carregar()} />
                  </li>
                ))}
              </ul>
            </section>
          )
        })}

      {visao === 'semana' &&
        !carregando &&
        dias.map((dia) => {
          const doDia = itens.filter((i) => i.data === dia)
          return (
            <section className="secao-fotos" key={dia}>
              <h2>
                <button
                  type="button"
                  className="link"
                  onClick={() => {
                    setData(dia)
                    setVisao('dia')
                  }}
                >
                  {formatarDia(dia)}
                </button>
              </h2>
              {doDia.length === 0 && <p className="vazio">Sem reservas.</p>}
              <ul className="agenda">
                {doDia.map((item) => (
                  <li key={item.id}>
                    <span className="hora">
                      {horaCurta(item.horaInicio)}
                      <small>{item.quadraNome}</small>
                    </span>
                    <AcoesDaReserva item={item} aoMudar={() => void carregar()} />
                  </li>
                ))}
              </ul>
            </section>
          )
        })}

      {selecao && <LancarHorario key={selecao.horario.inicio} selecao={selecao} aoFechar={() => setSelecao(null)} aoLancar={aoLancar} />}
    </main>
  )
}

interface LancarProps {
  selecao: Selecao
  aoFechar: () => void
  aoLancar: () => void
}

function LancarHorario({ selecao, aoFechar, aoLancar }: LancarProps) {
  const { quadra, horario } = selecao
  const [tipo, setTipo] = useState<'reserva' | 'bloqueio'>('reserva')
  const [nome, setNome] = useState('')
  const [preco, setPreco] = useState(String(horario.preco))
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    const periodo = { quadraId: quadra.id, inicio: horario.inicio, fim: horario.fim }
    try {
      if (tipo === 'reserva') {
        await api('/api/dono/reservas', {
          method: 'POST',
          corpo: { ...periodo, clienteNome: nome, preco: Number(preco) },
        })
      } else {
        await api('/api/dono/bloqueios', { method: 'POST', corpo: { ...periodo, motivo: nome } })
      }
      aoLancar()
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <aside className="confirmacao">
      <p>
        <strong>{quadra.nome}</strong>, {horaCurta(horario.horaInicio)} às {horaCurta(horario.horaFim)}
      </p>
      <form onSubmit={aoEnviar}>
        <div className="linha">
          <label className="opcao">
            <input type="radio" name="tipo" checked={tipo === 'reserva'} onChange={() => setTipo('reserva')} />
            Reserva
          </label>
          <label className="opcao">
            <input type="radio" name="tipo" checked={tipo === 'bloqueio'} onChange={() => setTipo('bloqueio')} />
            Bloqueio
          </label>
        </div>
        <label>
          {tipo === 'reserva' ? 'Nome do cliente' : 'Motivo'}
          <input value={nome} onChange={(e) => setNome(e.target.value)} maxLength={120} required />
        </label>
        {tipo === 'reserva' && (
          <label>
            Valor (R$)
            <input type="number" min={0} step="0.01" value={preco} onChange={(e) => setPreco(e.target.value)} required />
          </label>
        )}
        {erro && <p className="erro" role="alert">{erro}</p>}
        <div className="linha">
          <button type="submit" className="cresce" disabled={enviando}>
            {enviando ? 'Salvando…' : 'Salvar'}
          </button>
          <button type="button" className="secundario" onClick={aoFechar}>
            Fechar
          </button>
        </div>
      </form>
    </aside>
  )
}
