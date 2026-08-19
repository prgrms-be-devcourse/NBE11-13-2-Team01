import { Navigate, Route, Routes } from 'react-router'
import { AdminRoute } from './components/AdminRoute'
import { AppLayout } from './components/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { CreatePlanPage } from './pages/CreatePlanPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PlanDetailPage } from './pages/PlanDetailPage'
import { PlanListPage } from './pages/PlanListPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/plans" replace />} />
          <Route path="/plans" element={<PlanListPage />} />
          <Route path="/plans/:planId" element={<PlanDetailPage />} />
          <Route element={<AdminRoute />}>
            <Route path="/plans/new" element={<CreatePlanPage />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}

export default App
