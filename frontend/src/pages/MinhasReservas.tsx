import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../api/client'
import type { Reserva, StatusReserva } from '../api/tipos'
import { formatarDataHora, formatarPreco } from '../formatos'

const ROTULO_STATUS: Record<StatusReserva, string> = {
  PENDENTE: 'Pendente',
  CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada',
  CONCLUIDA: 'Concluída',
  FALTOU: 'Falta registrada',
  BLOQUEADA: 'Bloqueada',
}

export default function MinhasReservas() {
  const [reservas, setReservas] = useState<Reserva[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ignorar = false
    api<Reserva[]>('/api/reservas/minhas')
      .then((lista) => {
        if (!ignorar) setReservas(lista)
      })
      .catch((e) => {
        if (!ignorar) setErro(mensagemDeErro(e))
      })
    return () => {
      ignorar = true
    }
  }, [])

  async function cancelar(reserva: Reserva) {
    if (!window.confirm(`Cancelar a reserva em ${reserva.empresaNome}?`)) return
    setErro(null)
    try {
      const atualizada = await api<Reserva>(`/api/reservas/${reserva.id}/cancelamento`, { method: 'POST' })
      setReservas((lista) => lista?.map((r) => (r.id === atualizada.id ? atualizada : r)) ?? null)
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  return (
    <main className="cartao largo">
      <h1>Minhas reservas</h1>
      {erro && <p className="erro" role="alert">{erro}</p>}
      {reservas === null && !erro && <p className="carregando">Carregando…</p>}
      {reservas?.length === 0 && (
        <p className="vazio">
          Você ainda não tem reservas. <Link to="/buscar">Encontre onde jogar</Link>.
        </p>
      )}
      {reservas && reservas.length > 0 && (
        <ul className="lista">
          {reservas.map((reserva) => (
            <li key={reserva.id}>
              <div className="cresce">
                <Link to={`/empresas/${reserva.empresaId}`}>{reserva.empresaNome}</Link>
                <small>
                  {reserva.quadraNome} · {formatarDataHora(reserva.inicio, reserva.fuso)} ·{' '}
                  {formatarPreco(reserva.precoTotal)}
                </small>
                <small>{reserva.endereco}</small>
                {reserva.podeCancelar && (
                  <small>Cancele pelo app até {formatarDataHora(reserva.canceladaAte, reserva.fuso)}</small>
                )}
                {reserva.status === 'CONFIRMADA' && !reserva.podeCancelar && (
                  <small>
                    Prazo de cancelamento encerrado. Fale com o local
                    {reserva.telefone && `: ${reserva.telefone}`}.
                  </small>
                )}
              </div>
              <div className="acoes-reserva">
                <span className="etiqueta">{ROTULO_STATUS[reserva.status]}</span>
                {reserva.podeCancelar && (
                  <button type="button" className="secundario" onClick={() => cancelar(reserva)}>
                    Cancelar
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
