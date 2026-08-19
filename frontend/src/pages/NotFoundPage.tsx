import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <main className="not-found-page">
      <span className="eyebrow">404 · LOST ROUTE</span>
      <h1>경로를 찾지 못했어요.</h1>
      <p>배송 경로를 다시 확인하고 계획 목록으로 돌아가세요.</p>
      <Link to="/plans" className="button button-primary">배송 계획으로 이동</Link>
    </main>
  )
}
