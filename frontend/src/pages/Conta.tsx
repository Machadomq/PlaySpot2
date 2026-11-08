import { useAuth } from '../auth/AuthContext'

const ROTULO_PAPEL = {
  JOGADOR: 'Jogador',
  DONO: 'Dono de quadras',
  ADMIN: 'Administrador',
} as const

export default function Conta() {
  const { usuario, sair } = useAuth()
  if (!usuario) return null

  return (
    <main className="cartao">
      <h1>Olá, {usuario.nome}</h1>
      <dl>
        <dt>E-mail</dt>
        <dd>{usuario.email}</dd>
        <dt>Perfil</dt>
        <dd>{ROTULO_PAPEL[usuario.papel]}</dd>
      </dl>
      <button type="button" className="secundario" onClick={sair}>
        Sair
      </button>
    </main>
  )
}
