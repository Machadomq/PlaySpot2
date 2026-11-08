import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { api, mensagemDeErro } from '../../api/client'
import type { EmpresaAdmin, Esporte, Metricas } from '../../api/tipos'

export default function Painel() {
  const [metricas, setMetricas] = useState<Metricas | null>(null)
  const [empresas, setEmpresas] = useState<EmpresaAdmin[]>([])
  const [esportes, setEsportes] = useState<Esporte[]>([])
  const [erro, setErro] = useState<string | null>(null)

  const [novoEsporte, setNovoEsporte] = useState('')
  const [emailSenha, setEmailSenha] = useState('')
  const [senhaGerada, setSenhaGerada] = useState<{ email: string; senha: string } | null>(null)

  const carregar = useCallback(async () => {
    try {
      const [m, e, s] = await Promise.all([
        api<Metricas>('/api/admin/metricas'),
        api<EmpresaAdmin[]>('/api/admin/empresas'),
        api<Esporte[]>('/api/esportes'),
      ])
      setMetricas(m)
      setEmpresas(e)
      setEsportes(s)
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }, [])

  useEffect(() => {
    void carregar()
  }, [carregar])

  /** Executa a ação, recarrega o painel e mostra o erro, se houver. */
  async function executar(acao: () => Promise<unknown>) {
    setErro(null)
    try {
      await acao()
      await carregar()
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  function aprovar(empresa: EmpresaAdmin, aprovada: boolean) {
    const acao = aprovada ? 'aprovacao' : 'revogacao'
    void executar(() => api(`/api/admin/empresas/${empresa.id}/${acao}`, { method: 'POST' }))
  }

  function criarEsporte(evento: FormEvent) {
    evento.preventDefault()
    void executar(async () => {
      await api('/api/admin/esportes', { method: 'POST', corpo: { nome: novoEsporte } })
      setNovoEsporte('')
    })
  }

  function renomearEsporte(esporte: Esporte) {
    const nome = window.prompt('Novo nome do esporte', esporte.nome)
    if (!nome || nome === esporte.nome) return
    void executar(() => api(`/api/admin/esportes/${esporte.id}`, { method: 'PUT', corpo: { nome } }))
  }

  function removerEsporte(esporte: Esporte) {
    if (!window.confirm(`Remover ${esporte.nome}?`)) return
    void executar(() => api(`/api/admin/esportes/${esporte.id}`, { method: 'DELETE' }))
  }

  async function gerarSenha(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setSenhaGerada(null)
    if (!window.confirm(`A senha atual de ${emailSenha} deixará de funcionar. Continuar?`)) return
    try {
      const resposta = await api<{ senhaTemporaria: string }>('/api/admin/usuarios/senha-temporaria', {
        method: 'POST',
        corpo: { email: emailSenha },
      })
      setSenhaGerada({ email: emailSenha, senha: resposta.senhaTemporaria })
      setEmailSenha('')
    } catch (e) {
      setErro(mensagemDeErro(e))
    }
  }

  return (
    <main className="cartao largo">
      <h1>Administração</h1>
      {erro && <p className="erro" role="alert">{erro}</p>}

      {metricas && (
        <dl className="indicadores">
          <div><dt>Jogadores</dt><dd>{metricas.jogadores}</dd></div>
          <div><dt>Donos</dt><dd>{metricas.donos}</dd></div>
          <div><dt>Empresas aprovadas</dt><dd>{metricas.empresasAprovadas}</dd></div>
          <div><dt>Empresas pendentes</dt><dd>{metricas.empresasPendentes}</dd></div>
          <div><dt>Reservas (30 dias)</dt><dd>{metricas.reservasConfirmadas}</dd></div>
          <div><dt>Faltas (30 dias)</dt><dd>{metricas.faltas}</dd></div>
          <div><dt>Canceladas (30 dias)</dt><dd>{metricas.canceladas}</dd></div>
        </dl>
      )}

      <section className="secao-fotos">
        <h2>Empresas</h2>
        {empresas.length === 0 && <p className="vazio">Nenhuma empresa cadastrada.</p>}
        <ul className="lista">
          {empresas.map((empresa) => (
            <li key={empresa.id}>
              <div className="cresce">
                <strong>{empresa.nome}</strong>
                <small>
                  {empresa.cidade}/{empresa.uf}
                  {empresa.documento && ` · ${empresa.documento}`}
                </small>
                <small>{empresa.donoNome} · {empresa.donoEmail}</small>
              </div>
              {empresa.aprovada ? (
                <button type="button" className="secundario compacto" onClick={() => aprovar(empresa, false)}>
                  Revogar
                </button>
              ) : (
                <button type="button" className="compacto" onClick={() => aprovar(empresa, true)}>
                  Aprovar
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="secao-fotos">
        <h2>Esportes</h2>
        <form onSubmit={criarEsporte} className="linha">
          <label className="cresce">
            Novo esporte
            <input value={novoEsporte} onChange={(e) => setNovoEsporte(e.target.value)} maxLength={60} required />
          </label>
          <button type="submit" className="alinhado">Adicionar</button>
        </form>
        <ul className="lista">
          {esportes.map((esporte) => (
            <li key={esporte.id}>
              <span className="cresce">{esporte.nome}</span>
              <button type="button" className="secundario compacto" onClick={() => renomearEsporte(esporte)}>
                Renomear
              </button>
              <button type="button" className="secundario compacto" onClick={() => removerEsporte(esporte)}>
                Remover
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className="secao-fotos">
        <h2>Recuperar acesso de um usuário</h2>
        <form onSubmit={gerarSenha} className="linha">
          <label className="cresce">
            E-mail do usuário
            <input type="email" value={emailSenha} onChange={(e) => setEmailSenha(e.target.value)} required />
          </label>
          <button type="submit" className="alinhado">Gerar senha</button>
        </form>
        {senhaGerada && (
          <p className="aviso resultado-vazio">
            Senha temporária de {senhaGerada.email}: <code>{senhaGerada.senha}</code>. Ela só aparece agora. Entregue
            à pessoa e peça que troque em Conta.
          </p>
        )}
      </section>
    </main>
  )
}
