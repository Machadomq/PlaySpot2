const CHAVE_TOKEN = 'playspot.token'

// O armazenamento do navegador pode estar bloqueado (aba anônima, política do navegador)
export function lerToken(): string | null {
  try {
    return localStorage.getItem(CHAVE_TOKEN)
  } catch {
    return null
  }
}

export function guardarToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(CHAVE_TOKEN, token)
    else localStorage.removeItem(CHAVE_TOKEN)
  } catch {
    // sem armazenamento: o usuário só fica logado até recarregar a página
  }
}

export class ApiError extends Error {
  status: number

  constructor(status: number, mensagem: string) {
    super(mensagem)
    this.status = status
  }
}

function mensagemPorStatus(status: number): string {
  switch (status) {
    case 400:
      return 'Confira os dados informados.'
    case 401:
      return 'E-mail ou senha inválidos.'
    case 409:
      return 'Este e-mail já está cadastrado.'
    default:
      return 'Algo deu errado. Tente novamente em instantes.'
  }
}

interface Opcoes {
  method?: 'GET' | 'POST'
  corpo?: unknown
}

export async function api<T>(caminho: string, { method = 'GET', corpo }: Opcoes = {}): Promise<T> {
  const token = lerToken()
  const cabecalhos: Record<string, string> = {}
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'
  if (token) cabecalhos['Authorization'] = `Bearer ${token}`

  let resposta: Response
  try {
    resposta = await fetch(caminho, {
      method,
      headers: cabecalhos,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Não foi possível falar com o servidor.')
  }

  if (!resposta.ok) {
    throw new ApiError(resposta.status, mensagemPorStatus(resposta.status))
  }
  return (await resposta.json()) as T
}
