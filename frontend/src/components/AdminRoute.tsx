import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext'

export function AdminRoute() {
  const { user } = useAuth()

  if (user?.role !== 'ROLE_ADMIN') {
    return <Navigate to="/plans" replace />
  }

  return <Outlet />
}
