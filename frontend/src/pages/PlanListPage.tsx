import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { getAllDeliveryPlans, getDeliveryPlans } from '../api/deliveryPlans'
import { useAuth } from '../auth/AuthContext'
import { StatusBadge } from '../components/StatusBadge'
import type { AdminDeliveryPlanSummary, DeliveryPlanSummary } from '../types/api'
import { errorMessage, formatDateTime } from '../utils/format'

type PlanRow = DeliveryPlanSummary | AdminDeliveryPlanSummary

function isAdminPlan(plan: PlanRow): plan is AdminDeliveryPlanSummary {
  return 'driverId' in plan
}

export function PlanListPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ROLE_ADMIN'
  const [plans, setPlans] = useState<PlanRow[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const loadPlans = useCallback(async () => {
    setError('')
    setIsLoading(true)
    try {
      setPlans(isAdmin ? await getAllDeliveryPlans() : await getDeliveryPlans())
    } catch (caughtError) {
      setError(errorMessage(caughtError))
    } finally {
      setIsLoading(false)
    }
  }, [isAdmin])

  useEffect(() => {
    // oxlint-disable-next-line react/set-state-in-effect -- 목록 데이터는 화면 진입 시 불러온다.
    void loadPlans()
  }, [loadPlans])

  const summary = useMemo(
    () => ({
      active: plans.filter((plan) => plan.status === 'DELIVERING').length,
      ready: plans.filter((plan) => plan.status === 'READY').length,
      danger: plans.reduce((count, plan) => count + plan.dangerStops, 0),
    }),
    [plans],
  )

  return (
    <div className="page-stack">
      <section className="page-heading heading-with-action">
        <div>
          <span className="eyebrow">DELIVERY OVERVIEW</span>
          <h1>{isAdmin ? '전체 배송 계획' : '내 배송 계획'}</h1>
          <p>
            {isAdmin
              ? '기사별 배송 진행 상태와 기상 위험도를 한눈에 확인하세요.'
              : '오늘 방문할 배송지와 기상 위험도를 확인하세요.'}
          </p>
        </div>
        {isAdmin && (
          <Link to="/plans/new" className="button button-primary">+ 배송 계획 할당</Link>
        )}
      </section>

      <section className="summary-grid" aria-label="배송 요약">
        <article className="summary-card summary-primary">
          <span>{isAdmin ? '전체 진행 중' : '진행 중인 계획'}</span>
          <strong>{summary.active}</strong>
          <small>현재 배송 중</small>
        </article>
        <article className="summary-card">
          <span>출발 대기</span>
          <strong>{summary.ready}</strong>
          <small>순서 변경 가능</small>
        </article>
        <article className="summary-card summary-danger">
          <span>위험 배송지</span>
          <strong>{summary.danger}</strong>
          <small>주의가 필요한 지점</small>
        </article>
      </section>

      {error && (
        <div className="alert alert-error alert-with-action">
          <span>{error}</span>
          <button type="button" onClick={() => void loadPlans()}>다시 시도</button>
        </div>
      )}

      {isLoading ? (
        <div className="content-state">배송 계획을 불러오고 있어요.</div>
      ) : plans.length === 0 ? (
        <section className="empty-state">
          <div className="empty-icon">↗</div>
          <h2>아직 배송 계획이 없어요</h2>
          <p>
            {isAdmin
              ? '배송 기사에게 첫 계획을 할당해 보세요.'
              : '관리자가 계획을 할당하면 이곳에서 확인할 수 있어요.'}
          </p>
          {isAdmin && (
            <Link to="/plans/new" className="button button-primary">배송 계획 할당</Link>
          )}
        </section>
      ) : (
        <section className="plan-list" aria-label="배송 계획 목록">
          {plans.map((plan) => (
            <Link to={`/plans/${plan.planId}`} className="plan-card" key={plan.planId}>
              <div className="plan-card-main">
                <div className="plan-number">#{String(plan.planId).padStart(3, '0')}</div>
                <div>
                  <div className="plan-card-title-row">
                    <h2>{plan.departureLocation}</h2>
                    <StatusBadge status={plan.status} />
                  </div>
                  <p className="plan-time">예정 출발 · {formatDateTime(plan.scheduledDepartureAt)}</p>
                  {isAdminPlan(plan) && (
                    <p className="plan-driver">
                      담당 기사 · <strong>{plan.driverName}</strong> ({plan.driverLoginId})
                    </p>
                  )}
                </div>
              </div>
              <div className="plan-stats">
                <span><strong>{plan.remainingStops}</strong> / {plan.totalStops} 남음</span>
                <span className={plan.dangerStops > 0 ? 'danger-text' : ''}>
                  위험 <strong>{plan.dangerStops}</strong>
                </span>
                <span className="arrow-link">→</span>
              </div>
            </Link>
          ))}
        </section>
      )}
    </div>
  )
}
