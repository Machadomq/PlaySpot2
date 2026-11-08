import { useState } from 'react'
import type { FormEvent } from 'react'
import { api, mensagemDeErro } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const ROTULO_PAPEL = {
  JOGADOR: 'Jogador',
  DONO: 'Dono de quadras',
  ADMIN: 'Administrador',
} as const

export default function Conta() {
  const { usuario } = useAuth()
  const [senhaAtual, setSenhaAtual] = useState('')
  const [novaSenha, setNovaSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [alterada, setAlterada] = useState(false)
  const [enviando, setEnviando] = useState(false)

  if (!usuario) return null

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setAlterada(false)
    setEnviando(true)
    try {
      await api<void>('/api/me/senha', { method: 'POST', corpo: { senhaAtual, novaSenha } })
      setAlterada(true)
      setSenhaAtual('')
      setNovaSenha('')
    } catch (e) {
      setErro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="cartao">
      <h1>Olá, {usuario.nome}</h1>
      <dl>
        <dt>E-mail</dt>
        <dd>{usuario.email}</dd>
        <dt>Perfil</dt>
        <dd>{ROTULO_PAPEL[usuario.papel]}</dd>
      </dl>

      <section className="secao-fotos">
        <h2>Trocar senha</h2>
        <form onSubmit={aoEnviar}>
          <label>
            Senha atual
            <input
              type="password"
              value={senhaAtual}
              onChange={(e) => setSenhaAtual(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          <label>
            Nova senha (mínimo 8 caracteres)
            <input
              type="password"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              autoComplete="new-password"
              minLength={8}
              maxLength={72}
              required
            />
          </label>
          {erro && <p className="erro" role="alert">{erro}</p>}
          {alterada && <p className="sucesso" role="status">Senha alterada.</p>}
          <button type="submit" disabled={enviando}>
            {enviando ? 'Salvando…' : 'Trocar senha'}
          </button>
        </form>
      </section>
    </main>
  )
}
