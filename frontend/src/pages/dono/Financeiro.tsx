import { useEffect, useState } from 'react'
import { api, mensagemDeErro } from '../../api/client'
import type { MetodoPagamento, ResumoFinanceiro } from '../../api/tipos'
import { formatarPreco, hoje, primeiroDiaDoMes, ultimoDiaDoMes } from '../../formatos'

const ROTULO_METODO: Record<MetodoPagamento, string> = {
  PIX: 'Pix',
  CARTAO: 'Cartão',
  DINHEIRO: 'Dinheiro',
}

export default function Financeiro() {
  const [de, setDe] = useState(() => primeiroDiaDoMes(hoje()))
  const [ate, setAte] = useState(() => ultimoDiaDoMes(hoje()))
  const [resumo, setResumo] = useState<ResumoFinanceiro | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!de || !ate) return
    let ignorar = false
    api<ResumoFinanceiro>(`/api/dono/financeiro?de=${de}&ate=${ate}`)
      .then((dados) => {
        if (ignorar) return
        setResumo(dados)
        setErro(null)
      })
      .catch((e) => {
        if (ignorar) return
        setResumo(null)
        setErro(mensagemDeErro(e))
      })
    return () => {
      ignorar = true
    }
  }, [de, ate])

  return (
    <main className="cartao largo">
      <h1>Financeiro</h1>
      <div className="linha">
        <label className="cresce">
          De
          <input type="date" value={de} onChange={(e) => setDe(e.target.value)} />
        </label>
        <label className="cresce">
          Até
          <input type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
        </label>
      </div>
      {erro && <p className="erro resultado-vazio" role="alert">{erro}</p>}
      {resumo && (
        <>
          <dl className="indicadores">
            <div>
              <dt>Recebido</dt>
              <dd>{formatarPreco(resumo.recebido)}</dd>
            </div>
            <div>
              <dt>A receber</dt>
              <dd>{formatarPreco(resumo.aReceber)}</dd>
            </div>
            <div>
              <dt>Perdido em faltas</dt>
              <dd>{formatarPreco(resumo.perdidoEmFaltas)}</dd>
            </div>
            <div>
              <dt>Reservas</dt>
              <dd>{resumo.reservas}</dd>
            </div>
            <div>
              <dt>Faltas</dt>
              <dd>{resumo.faltas}</dd>
            </div>
            <div>
              <dt>Canceladas</dt>
              <dd>{resumo.canceladas}</dd>
            </div>
          </dl>
          {Object.keys(resumo.recebidoPorMetodo).length > 0 && (
            <section className="secao-fotos">
              <h2>Recebido por forma de pagamento</h2>
              <ul className="lista">
                {(Object.entries(resumo.recebidoPorMetodo) as [MetodoPagamento, number][]).map(([metodo, valor]) => (
                  <li key={metodo}>
                    <span className="cresce">{ROTULO_METODO[metodo]}</span>
                    <strong>{formatarPreco(valor)}</strong>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <p className="rodape">Valores pela data do jogo.</p>
        </>
      )}
    </main>
  )
}
