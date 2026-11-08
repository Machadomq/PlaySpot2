import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { usuario, sair } = useAuth()

  return (
    <>
      <header className="topo">
        <Link to="/" className="marca">
          PlaySpot
        </Link>
        <nav>
          {usuario?.papel !== 'DONO' && <NavLink to="/buscar">Buscar</NavLink>}
          {usuario?.papel === 'JOGADOR' && <NavLink to="/reservas">Reservas</NavLink>}
          {usuario?.papel === 'ADMIN' && <NavLink to="/admin">Administração</NavLink>}
          {usuario?.papel === 'DONO' && (
            <>
              <NavLink to="/dono/agenda">Agenda</NavLink>
              <NavLink to="/dono/quadras">Quadras</NavLink>
              <NavLink to="/dono/financeiro">Financeiro</NavLink>
              <NavLink to="/dono/empresa">Empresa</NavLink>
            </>
          )}
          {usuario ? (
            <>
              <NavLink to="/conta">Conta</NavLink>
              <button type="button" className="secundario" onClick={sair}>
                Sair
              </button>
            </>
          ) : (
            <NavLink to="/login">Entrar</NavLink>
          )}
        </nav>
      </header>
      <Outlet />
    </>
  )
}
