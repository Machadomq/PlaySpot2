import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import type { Papel } from './api/tipos'
import { useAuth } from './auth/AuthContext'
import Layout from './components/Layout'
import Cadastro from './pages/Cadastro'
import Conta from './pages/Conta'
import Login from './pages/Login'
import MinhaEmpresa from './pages/dono/MinhaEmpresa'
import QuadraFormulario from './pages/dono/QuadraFormulario'
import Quadras from './pages/dono/Quadras'

/** Só deixa passar quem está logado; os demais vão para o login. */
function Protegida({ children, papel }: { children: ReactNode; papel?: Papel }) {
  const { usuario, carregando } = useAuth()
  if (carregando) return <p className="carregando">Carregando…</p>
  if (!usuario) return <Navigate to="/login" replace />
  if (papel && usuario.papel !== papel) return <Navigate to="/" replace />
  return children
}

function RotaDono({ children }: { children: ReactNode }) {
  return <Protegida papel="DONO">{children}</Protegida>
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route
          path="/"
          element={
            <Protegida>
              <Conta />
            </Protegida>
          }
        />
        <Route path="/dono/empresa" element={<RotaDono><MinhaEmpresa /></RotaDono>} />
        <Route path="/dono/quadras" element={<RotaDono><Quadras /></RotaDono>} />
        <Route path="/dono/quadras/nova" element={<RotaDono><QuadraFormulario /></RotaDono>} />
        <Route path="/dono/quadras/:id" element={<RotaDono><QuadraFormulario /></RotaDono>} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
