import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import CenaQuadra from '../components/CenaQuadra'

export default function Login() {
  const { usuario, entrar } = useAuth()
  const local = useLocation()
  // volta para a página de onde o jogador veio, por exemplo a de uma quadra que ele ia reservar
  const destino = (local.state as { de?: string } | null)?.de ?? '/'
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to={destino} replace />

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await entrar(email, senha)
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Algo deu errado.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="auth">
    <aside className="auth-arte">
      <CenaQuadra />
      <p>Sua próxima partida começa aqui.</p>
    </aside>
    <main className="cartao">
      <h1>Que bom te ver!</h1>
      <form onSubmit={aoEnviar}>
        <label>
          E-mail
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
        </label>
        <label>
          Senha
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <p className="rodape">
        Ainda não tem conta? <Link to="/cadastro" state={{ de: destino }}>Cadastre-se</Link>
      </p>
    </main>
    </div>
  )
}
