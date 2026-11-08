import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, mensagemDeErro } from '../../api/client'
import type { FaixaHorario, HorarioDisponivel, Quadra } from '../../api/tipos'
import { formatarPreco, hoje } from '../../formatos'

const DIAS = [
  { numero: 1, nome: 'Segunda' },
  { numero: 2, nome: 'Terça' },
  { numero: 3, nome: 'Quarta' },
  { numero: 4, nome: 'Quinta' },
  { numero: 5, nome: 'Sexta' },
  { numero: 6, nome: 'Sábado' },
  { numero: 0, nome: 'Domingo' },
]
const DIAS_UTEIS = [1, 2, 3, 4, 5]

interface FaixaEditavel {
  chave: number
  horaInicio: string
  horaFim: string
  precoHora: string
}

type Semana = FaixaEditavel[][]

let proximaChave = 0
function novaChave() {
  proximaChave += 1
  return proximaChave
}

const semanaVazia = (): Semana => Array.from({ length: 7 }, () => [])

export default function HorariosQuadra() {
  const { id } = useParams()
  const [nomeQuadra, setNomeQuadra] = useState<string | null>(null)
  const [semana, setSemana] = useState<Semana>(semanaVazia)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [salva, setSalva] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const [dataPrevia, setDataPrevia] = useState(hoje)
  const [previa, setPrevia] = useState<HorarioDisponivel[] | null>(null)
  const [erroPrevia, setErroPrevia] = useState<string | null>(null)
  const [versaoSalva, setVersaoSalva] = useState(0)

  useEffect(() => {
    let ignorar = false
    Promise.all([api<FaixaHorario[]>(`/api/dono/quadras/${id}/horarios`), api<Quadra[]>('/api/dono/quadras')])
      .then(([faixas, quadras]) => {
        if (ignorar) return
        const nova = semanaVazia()
        for (const faixa of faixas) {
          nova[faixa.diaSemana].push({
            chave: novaChave(),
            horaInicio: faixa.horaInicio.slice(0, 5),
            horaFim: faixa.horaFim.slice(0, 5),
            precoHora: String(faixa.precoHora),
          })
        }
        setSemana(nova)
        setNomeQuadra(quadras.find((q) => q.id === Number(id))?.nome ?? '')
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
  }, [id])

  useEffect(() => {
    if (!dataPrevia) return
    let ignorar = false
    api<HorarioDisponivel[]>(`/api/dono/quadras/${id}/grade?data=${dataPrevia}`)
      .then((lista) => {
        if (ignorar) return
        setPrevia(lista)
        setErroPrevia(null)
      })
      .catch((e) => {
        if (ignorar) return
        setPrevia(null)
        setErroPrevia(mensagemDeErro(e))
      })
    return () => {
      ignorar = true
    }
  }, [id, dataPrevia, versaoSalva])

  function alterarDia(dia: number, faixas: FaixaEditavel[]) {
    setSemana((atual) => atual.map((doDia, i) => (i === dia ? faixas : doDia)))
    setSalva(false)
  }

  function adicionar(dia: number) {
    const faixas = semana[dia]
    const ultima = faixas.at(-1)
    alterarDia(dia, [
      ...faixas,
      { chave: novaChave(), horaInicio: ultima?.horaFim ?? '08:00', horaFim: '', precoHora: ultima?.precoHora ?? '' },
    ])
  }

  function alterarFaixa(dia: number, chave: number, campos: Partial<FaixaEditavel>) {
    alterarDia(dia, semana[dia].map((f) => (f.chave === chave ? { ...f, ...campos } : f)))
  }

  function remover(dia: number, chave: number) {
    alterarDia(dia, semana[dia].filter((f) => f.chave !== chave))
  }

  function copiar(origem: number, destinos: number[]) {
    setSemana((atual) =>
      atual.map((doDia, dia) =>
        destinos.includes(dia) && dia !== origem
          ? atual[origem].map((f) => ({ ...f, chave: novaChave() }))
          : doDia,
      ),
    )
    setSalva(false)
  }

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setSalva(false)
    setEnviando(true)
    try {
      const faixas = semana.flatMap((doDia, diaSemana) =>
        doDia.map((f) => ({
          diaSemana,
          horaInicio: f.horaInicio,
          horaFim: f.horaFim,
          precoHora: Number(f.precoHora),
        })),
      )
      await api<FaixaHorario[]>(`/api/dono/quadras/${id}/horarios`, { method: 'PUT', corpo: { faixas } })
      setSalva(true)
      setVersaoSalva((v) => v + 1)
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  if (carregando) return <p className="carregando">Carregando…</p>

  return (
    <main className="cartao largo">
      <h1>Horários e preços{nomeQuadra ? `: ${nomeQuadra}` : ''}</h1>
      <p className="vazio">Use 00:00 como fim da faixa para indicar que ela vai até a meia-noite.</p>
      <form onSubmit={aoEnviar}>
        {DIAS.map(({ numero, nome }) => (
          <fieldset key={numero}>
            <legend>{nome}</legend>
            {semana[numero].length === 0 && <p className="vazio">Fechado</p>}
            {semana[numero].map((faixa) => (
              <div className="faixa" key={faixa.chave}>
                <label>
                  Início
                  <input
                    type="time"
                    value={faixa.horaInicio}
                    onChange={(e) => alterarFaixa(numero, faixa.chave, { horaInicio: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Fim
                  <input
                    type="time"
                    value={faixa.horaFim}
                    onChange={(e) => alterarFaixa(numero, faixa.chave, { horaFim: e.target.value })}
                    required
                  />
                </label>
                <label>
                  R$/hora
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={faixa.precoHora}
                    onChange={(e) => alterarFaixa(numero, faixa.chave, { precoHora: e.target.value })}
                    required
                  />
                </label>
                <button
                  type="button"
                  className="secundario"
                  aria-label={`Remover faixa de ${nome}`}
                  onClick={() => remover(numero, faixa.chave)}
                >
                  Remover
                </button>
              </div>
            ))}
            <div className="acoes-dia">
              <button type="button" className="secundario" onClick={() => adicionar(numero)}>
                Adicionar faixa
              </button>
              {semana[numero].length > 0 && (
                <>
                  <button type="button" className="secundario" onClick={() => copiar(numero, DIAS_UTEIS)}>
                    Copiar para seg a sex
                  </button>
                  <button type="button" className="secundario" onClick={() => copiar(numero, [0, 1, 2, 3, 4, 5, 6])}>
                    Copiar para todos
                  </button>
                </>
              )}
            </div>
          </fieldset>
        ))}
        {erro && <p className="erro" role="alert">{erro}</p>}
        {salva && <p className="sucesso" role="status">Horários salvos.</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : 'Salvar semana'}
        </button>
      </form>

      <section className="secao-fotos">
        <h2>Como o jogador vai ver</h2>
        <label>
          Data
          <input type="date" value={dataPrevia} onChange={(e) => setDataPrevia(e.target.value)} />
        </label>
        {erroPrevia && <p className="erro" role="alert">{erroPrevia}</p>}
        {previa?.length === 0 && <p className="vazio">Sem horários nesta data.</p>}
        {previa && previa.length > 0 && (
          <ul className="horarios">
            {previa.map((horario) => (
              <li key={horario.inicio} className={horario.livre ? '' : 'indisponivel'}>
                <strong>
                  {horario.horaInicio.slice(0, 5)} às {horario.horaFim.slice(0, 5)}
                </strong>
                <span>{horario.livre ? formatarPreco(horario.preco) : 'Indisponível'}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <p className="rodape">
        <Link to={`/dono/quadras/${id}`}>Voltar para a quadra</Link>
      </p>
    </main>
  )
}
