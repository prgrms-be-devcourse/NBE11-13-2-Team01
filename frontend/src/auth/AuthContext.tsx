import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import * as authApi from '../api/auth'
import {
  AUTH_UNAUTHORIZED_EVENT,
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '../api/client'
import type { UserInfo } from '../types/api'

interface AuthContextValue {
  user: UserInfo | null
  isAuthenticated: boolean
  isBootstrapping: boolean
  login: (loginId: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserInfo | null>(null)
  const [isBootstrapping, setIsBootstrapping] = useState(Boolean(getAccessToken()))

  const resetAuth = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  const loadUser = useCallback(async () => {
    const currentUser = await authApi.getMyInfo()
    setUser(currentUser)
  }, [])

  useEffect(() => {
    const handleUnauthorized = () => resetAuth()
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)

    if (getAccessToken()) {
      // oxlint-disable-next-line react/set-state-in-effect -- 사용자 세션 복원은 최초 마운트 시 실행한다.
      loadUser()
        .catch(resetAuth)
        .finally(() => setIsBootstrapping(false))
    }

    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [loadUser, resetAuth])

  const login = useCallback(
    async (loginId: string, password: string) => {
      const token = await authApi.login(loginId, password)
      setAccessToken(token.accessToken)
      try {
        await loadUser()
      } catch (error) {
        resetAuth()
        throw error
      }
    },
    [loadUser, resetAuth],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      resetAuth()
    }
  }, [resetAuth])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user && getAccessToken()),
      isBootstrapping,
      login,
      logout,
    }),
    [isBootstrapping, login, logout, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- Provider와 전용 hook이 같은 Context를 공유한다.
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth는 AuthProvider 안에서 사용해야 합니다.')
  return context
}
