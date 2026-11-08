import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './auth/AuthContext'
import Layout from './components/Layout'
import Cadastro from './pages/Cadastro'
import Conta from './pages/Conta'
import Login from './pages/Login'

/** Só deixa passar quem está logado; os demais vão para o login. */
function Protegida({ children }: { children: ReactNode }) {
  const { usuario, carregando } = useAuth()
  if (carregando) return <p className="carregando">Carregando…</p>
  if (!usuario) return <Navigate to="/login" replace />
  return children
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
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
