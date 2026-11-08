import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, mensagemDeErro } from '../../api/client'
import type { Quadra } from '../../api/tipos'

export default function Quadras() {
  const [quadras, setQuadras] = useState<Quadra[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ignorar = false
    api<Quadra[]>('/api/dono/quadras')
      .then((lista) => {
        if (!ignorar) setQuadras(lista)
      })
      .catch((e) => {
        if (!ignorar) setErro(mensagemDeErro(e))
      })
    return () => {
      ignorar = true
    }
  }, [])

  return (
    <main className="cartao largo">
      <div className="cabecalho-secao">
        <h1>Quadras</h1>
        <Link to="/dono/quadras/nova" className="botao">
          Nova quadra
        </Link>
      </div>
      {erro && <p className="erro" role="alert">{erro}</p>}
      {quadras === null && !erro && <p className="carregando">Carregando…</p>}
      {quadras?.length === 0 && (
        <p className="vazio">
          Nenhuma quadra ainda. Cadastre sua <Link to="/dono/empresa">empresa</Link> e crie a primeira.
        </p>
      )}
      {quadras && quadras.length > 0 && (
        <ul className="lista">
          {quadras.map((quadra) => (
            <li key={quadra.id}>
              {quadra.fotoIds.length > 0 ? (
                <img src={`/api/fotos/${quadra.fotoIds[0]}`} alt="" />
              ) : (
                <div className="sem-foto" aria-hidden="true" />
              )}
              <div className="cresce">
                <Link to={`/dono/quadras/${quadra.id}`}>{quadra.nome}</Link>
                <small>{quadra.esportes.map((esporte) => esporte.nome).join(', ')}</small>
              </div>
              {!quadra.ativa && <span className="etiqueta">Inativa</span>}
            </li>
          ))}
        </ul>
      )}
    </main>
  )
}
