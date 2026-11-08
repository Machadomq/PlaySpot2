export type Papel = 'JOGADOR' | 'DONO' | 'ADMIN'

export interface Usuario {
  id: number
  nome: string
  email: string
  telefone: string | null
  papel: Papel
}

export interface LoginResposta {
  token: string
  tipo: string
  expiraEmSegundos: number
  usuario: Usuario
}

export interface DadosCadastro {
  nome: string
  email: string
  senha: string
  telefone?: string
  papel: 'JOGADOR' | 'DONO'
}
