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

export interface DadosEmpresa {
  nome: string
  documento: string | null
  telefone: string | null
  endereco: string
  cidade: string
  uf: string
  prazoCancelamentoHoras: number
}

export interface Empresa extends DadosEmpresa {
  id: number
}

export interface Esporte {
  id: number
  nome: string
}

export interface DadosQuadra {
  nome: string
  descricao: string | null
  duracaoSlotMin: number
  ativa: boolean
  esporteIds: number[]
}

export interface Quadra {
  id: number
  nome: string
  descricao: string | null
  duracaoSlotMin: number
  ativa: boolean
  esportes: Esporte[]
  fotoIds: number[]
}

export interface DadosCadastro {
  nome: string
  email: string
  senha: string
  telefone?: string
  papel: 'JOGADOR' | 'DONO'
}
