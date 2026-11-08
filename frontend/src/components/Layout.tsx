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
              {usuario.papel === 'DONO' && (
                <>
                  <NavLink to="/dono/empresa">Empresa</NavLink>
                  <NavLink to="/dono/quadras">Quadras</NavLink>
                </>
              )}
              <NavLink to="/" end>
                Conta
              </NavLink>
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
