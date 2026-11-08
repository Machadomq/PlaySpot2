import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import type { DadosCadastro } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'

export default function Cadastro() {
  const { usuario, cadastrar } = useAuth()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')
  const [senha, setSenha] = useState('')
  const [papel, setPapel] = useState<DadosCadastro['papel']>('JOGADOR')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to="/" replace />

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await cadastrar({
        nome,
        email,
        senha,
        papel,
        telefone: telefone.trim() || undefined,
      })
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : 'Algo deu errado.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="cartao">
      <h1>Criar conta</h1>
      <form onSubmit={aoEnviar}>
        <fieldset>
          <legend>Eu quero…</legend>
          <label className="opcao">
            <input
              type="radio"
              name="papel"
              checked={papel === 'JOGADOR'}
              onChange={() => setPapel('JOGADOR')}
            />
            Reservar quadras
          </label>
          <label className="opcao">
            <input
              type="radio"
              name="papel"
              checked={papel === 'DONO'}
              onChange={() => setPapel('DONO')}
            />
            Anunciar minhas quadras
          </label>
        </fieldset>
        <label>
          Nome
          <input
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            autoComplete="name"
            maxLength={120}
            required
          />
        </label>
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
          Telefone (opcional)
          <input
            type="tel"
            value={telefone}
            onChange={(e) => setTelefone(e.target.value)}
            autoComplete="tel"
            maxLength={20}
          />
        </label>
        <label>
          Senha (mínimo 8 caracteres)
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="new-password"
            minLength={8}
            maxLength={72}
            required
          />
        </label>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Criando…' : 'Criar conta'}
        </button>
      </form>
      <p className="rodape">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </main>
  )
}
