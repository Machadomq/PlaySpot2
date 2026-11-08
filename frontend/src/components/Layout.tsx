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
          {usuario ? (
            <>
              <NavLink to="/">Minha conta</NavLink>
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
