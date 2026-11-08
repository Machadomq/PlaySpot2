import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, guardarToken, lerToken } from '../api/client'
import type { DadosCadastro, LoginResposta, Usuario } from '../api/tipos'

interface AuthState {
  usuario: Usuario | null
  /** true enquanto verificamos se o token guardado ainda é válido */
  carregando: boolean
  entrar: (email: string, senha: string) => Promise<void>
  cadastrar: (dados: DadosCadastro) => Promise<void>
  sair: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(() => lerToken() !== null)

  // Ao abrir o app: se há token guardado, confirma com o servidor quem é o usuário
  useEffect(() => {
    if (lerToken() === null) return
    api<Usuario>('/api/me')
      .then(setUsuario)
      .catch(() => guardarToken(null))
      .finally(() => setCarregando(false))
  }, [])

  const entrar = useCallback(async (email: string, senha: string) => {
    const resposta = await api<LoginResposta>('/api/auth/login', {
      method: 'POST',
      corpo: { email, senha },
    })
    guardarToken(resposta.token)
    setUsuario(resposta.usuario)
  }, [])

  const cadastrar = useCallback(
    async (dados: DadosCadastro) => {
      await api<Usuario>('/api/auth/cadastro', { method: 'POST', corpo: dados })
      await entrar(dados.email, dados.senha)
    },
    [entrar],
  )

  const sair = useCallback(() => {
    guardarToken(null)
    setUsuario(null)
  }, [])

  const valor = useMemo(
    () => ({ usuario, carregando, entrar, cadastrar, sair }),
    [usuario, carregando, entrar, cadastrar, sair],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  return contexto
}
