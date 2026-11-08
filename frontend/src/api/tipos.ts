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
  latitude: number | null
  longitude: number | null
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

export interface FaixaHorario {
  diaSemana: number
  horaInicio: string
  horaFim: string
  precoHora: number
}

export interface HorarioDisponivel {
  inicio: string
  fim: string
  horaInicio: string
  horaFim: string
  preco: number
  livre: boolean
}

export interface ResultadoBusca {
  id: number
  nome: string
  endereco: string
  cidade: string
  uf: string
  latitude: number | null
  longitude: number | null
  distanciaKm: number | null
  esportes: string[]
  fotoId: number | null
  horariosLivres: number
  precoAPartirDe: number
}

export interface EmpresaPublica {
  id: number
  nome: string
  telefone: string | null
  endereco: string
  cidade: string
  uf: string
  latitude: number | null
  longitude: number | null
  prazoCancelamentoHoras: number
  quadras: Quadra[]
}

export type StatusReserva = 'PENDENTE' | 'CONFIRMADA' | 'CANCELADA' | 'CONCLUIDA' | 'FALTOU' | 'BLOQUEADA'

export type MetodoPagamento = 'PIX' | 'CARTAO' | 'DINHEIRO'

export interface AgendaItem {
  id: number
  quadraId: number
  quadraNome: string
  inicio: string
  fim: string
  data: string
  horaInicio: string
  horaFim: string
  cliente: string
  telefone: string | null
  peloApp: boolean
  preco: number
  status: StatusReserva
  metodoPago: MetodoPagamento | null
}

export interface ResumoFinanceiro {
  recebido: number
  recebidoPorMetodo: Partial<Record<MetodoPagamento, number>>
  aReceber: number
  perdidoEmFaltas: number
  reservas: number
  faltas: number
  canceladas: number
}

export interface Reserva {
  id: number
  empresaId: number
  empresaNome: string
  endereco: string
  telefone: string | null
  quadraNome: string
  inicio: string
  fim: string
  fuso: string
  precoTotal: number
  status: StatusReserva
  canceladaAte: string
  podeCancelar: boolean
}

export interface DadosCadastro {
  nome: string
  email: string
  senha: string
  telefone?: string
  papel: 'JOGADOR' | 'DONO'
}
