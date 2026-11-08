import type { LucideIcon } from 'lucide-react'
import {
  Building2,
  CalendarDays,
  LayoutGrid,
  LogIn,
  LogOut,
  Search,
  ShieldCheck,
  Ticket,
  UserRound,
  Wallet,
} from 'lucide-react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import type { Papel } from '../api/tipos'
import { useAuth } from '../auth/AuthContext'
import Logo from './Logo'

interface Destino {
  para: string
  rotulo: string
  Icone: LucideIcon
}

const CONTA: Destino = { para: '/conta', rotulo: 'Conta', Icone: UserRound }
const BUSCAR: Destino = { para: '/buscar', rotulo: 'Buscar', Icone: Search }

const DESTINOS: Record<Papel, Destino[]> = {
  JOGADOR: [BUSCAR, { para: '/reservas', rotulo: 'Reservas', Icone: Ticket }, CONTA],
  DONO: [
    { para: '/dono/agenda', rotulo: 'Agenda', Icone: CalendarDays },
    { para: '/dono/quadras', rotulo: 'Quadras', Icone: LayoutGrid },
    { para: '/dono/financeiro', rotulo: 'Financeiro', Icone: Wallet },
    { para: '/dono/empresa', rotulo: 'Empresa', Icone: Building2 },
    CONTA,
  ],
  ADMIN: [BUSCAR, { para: '/admin', rotulo: 'Painel', Icone: ShieldCheck }, CONTA],
}

export default function Layout() {
  const { usuario, sair } = useAuth()
  const { pathname } = useLocation()
  const destinos = usuario ? DESTINOS[usuario.papel] : [BUSCAR, { para: '/login', rotulo: 'Entrar', Icone: LogIn }]

  const links = destinos.map(({ para, rotulo, Icone }) => (
    <NavLink key={para} to={para}>
      <Icone aria-hidden="true" />
      <span>{rotulo}</span>
    </NavLink>
  ))

  return (
    <>
      <a href="#conteudo" className="pular">
        Pular para o conteúdo
      </a>
      <header className="topo">
        <Link to="/" className="marca">
          <Logo />
        </Link>
        <nav className="menu" aria-label="Principal">
          {links}
        </nav>
        {usuario && (
          <button type="button" className="secundario sair" onClick={sair}>
            <LogOut aria-hidden="true" />
            <span>Sair</span>
          </button>
        )}
      </header>
      <div id="conteudo" key={pathname} className="transicao">
        <Outlet />
      </div>
      <nav className="abas" aria-label="Navegação inferior">
        {links}
      </nav>
    </>
  )
}
