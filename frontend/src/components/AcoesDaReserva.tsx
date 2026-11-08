import { useState } from 'react'
import { api, mensagemDeErro } from '../api/client'
import type { AgendaItem, MetodoPagamento } from '../api/tipos'
import { formatarPreco } from '../formatos'

const METODOS: { valor: MetodoPagamento; rotulo: string }[] = [
  { valor: 'PIX', rotulo: 'Pix' },
  { valor: 'CARTAO', rotulo: 'Cartão' },
  { valor: 'DINHEIRO', rotulo: 'Dinheiro' },
]

const rotuloMetodo = (metodo: MetodoPagamento) => METODOS.find((m) => m.valor === metodo)?.rotulo

interface Props {
  item: AgendaItem
  aoMudar: () => void
}

/** Linha de uma reserva ou bloqueio na agenda, com as ações que o estado atual permite. */
export default function AcoesDaReserva({ item, aoMudar }: Props) {
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [escolhendoMetodo, setEscolhendoMetodo] = useState(false)
  const [agora] = useState(() => Date.now())

  const bloqueio = item.status === 'BLOQUEADA'
  const jaComecou = new Date(item.inicio).getTime() <= agora

  async function executar(caminho: string, corpo?: unknown) {
    setErro(null)
    setEnviando(true)
    try {
      await api<AgendaItem>(`/api/dono/reservas/${item.id}/${caminho}`, { method: 'POST', corpo })
      setEscolhendoMetodo(false)
      aoMudar()
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  function cancelar() {
    const pergunta = bloqueio ? 'Liberar este horário?' : `Cancelar a reserva de ${item.cliente}?`
    if (window.confirm(pergunta)) void executar('cancelamento')
  }

  return (
    <div className="item-agenda">
      <div className="item-info">
        {bloqueio ? (
          <strong>Bloqueado: {item.cliente}</strong>
        ) : (
          <strong>
            {item.cliente}
            {item.peloApp && <span className="etiqueta">App</span>}
          </strong>
        )}
        {!bloqueio && (
          <small>
            {formatarPreco(item.preco)}
            {item.metodoPago && ` · Pago (${rotuloMetodo(item.metodoPago)})`}
            {item.status === 'FALTOU' && ' · Faltou'}
            {item.telefone && ` · ${item.telefone}`}
          </small>
        )}
      </div>
      <div className="item-acoes">
        {escolhendoMetodo ? (
          METODOS.map((metodo) => (
            <button
              key={metodo.valor}
              type="button"
              className="secundario"
              disabled={enviando}
              onClick={() => executar('pagamento', { metodo: metodo.valor })}
            >
              {metodo.rotulo}
            </button>
          ))
        ) : (
          <>
            {!bloqueio && !item.metodoPago && item.preco > 0 && (
              <button type="button" className="secundario" disabled={enviando} onClick={() => setEscolhendoMetodo(true)}>
                Marcar pago
              </button>
            )}
            {item.metodoPago && (
              <button type="button" className="secundario" disabled={enviando} onClick={() => executar('estorno')}>
                Estornar
              </button>
            )}
            {item.status === 'CONFIRMADA' && jaComecou && (
              <button type="button" className="secundario" disabled={enviando} onClick={() => executar('falta')}>
                Faltou
              </button>
            )}
            {item.status === 'FALTOU' && (
              <button type="button" className="secundario" disabled={enviando} onClick={() => executar('falta/desfazer')}>
                Desfazer falta
              </button>
            )}
            {(item.status === 'CONFIRMADA' || bloqueio) && !item.metodoPago && (
              <button type="button" className="secundario" disabled={enviando} onClick={cancelar}>
                {bloqueio ? 'Liberar' : 'Cancelar'}
              </button>
            )}
          </>
        )}
      </div>
      {erro && <p className="erro" role="alert">{erro}</p>}
    </div>
  )
}
