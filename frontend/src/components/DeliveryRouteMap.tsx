import L from 'leaflet'
import { useEffect, useMemo, useRef } from 'react'
import 'leaflet/dist/leaflet.css'
import type { DeliveryStop, RiskLevel } from '../types/api'

interface DeliveryRouteMapProps {
  departureAddress: string
  departureLatitude: number
  departureLongitude: number
  stops: DeliveryStop[]
  recommendedStopId?: number | null
}

interface RoutePoint {
  key: string
  label: string
  address: string
  latitude: number
  longitude: number
  departure: boolean
  completed: boolean
  riskLevel: RiskLevel | null
  fragile: boolean
}

const SEOUL_CENTER: L.LatLngExpression = [37.5665, 126.978]

export function DeliveryRouteMap({
  departureAddress,
  departureLatitude,
  departureLongitude,
  stops,
  recommendedStopId,
}: DeliveryRouteMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const routeLayerRef = useRef<L.LayerGroup | null>(null)

  const points = useMemo<RoutePoint[]>(() => [
    {
      key: 'departure',
      label: '출발',
      address: departureAddress,
      latitude: departureLatitude,
      longitude: departureLongitude,
      departure: true,
      completed: false,
      riskLevel: null,
      fragile: false,
    },
    ...stops.map((stop, index) => ({
      key: String(stop.stopId),
      label: String(index + 1),
      address: stop.address,
      latitude: stop.latitude,
      longitude: stop.longitude,
      departure: false,
      completed: stop.status === 'COMPLETED',
      riskLevel: stop.riskAssessment?.level ?? 'UNKNOWN',
      fragile: stop.deliveryItems.some((item) => item.productType === 'FRAGILE'),
    })),
  ].filter((point) => (
    Number.isFinite(point.latitude) && Number.isFinite(point.longitude)
  )), [departureAddress, departureLatitude, departureLongitude, stops])

  useEffect(() => {
    if (!containerRef.current) return

    const map = L.map(containerRef.current, {
      center: SEOUL_CENTER,
      zoom: 11,
      zoomControl: false,
      scrollWheelZoom: false,
    })

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map)
    L.control.zoom({ position: 'bottomright' }).addTo(map)

    mapRef.current = map
    routeLayerRef.current = L.layerGroup().addTo(map)

    return () => {
      map.remove()
      mapRef.current = null
      routeLayerRef.current = null
    }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    const routeLayer = routeLayerRef.current
    if (!map || !routeLayer || points.length === 0) return

    routeLayer.clearLayers()
    const coordinates: L.LatLngExpression[] = []

    points.forEach((point) => {
      const coordinate: L.LatLngExpression = [point.latitude, point.longitude]
      coordinates.push(coordinate)

      const recommended = !point.departure
        && Number(point.key) === recommendedStopId
      const riskClass = point.riskLevel
        ? ` risk-${point.riskLevel.toLowerCase()}`
        : ''
      const markerClass = `route-map-marker${point.departure ? ' departure-marker' : ''}${riskClass}${recommended ? ' recommended-marker' : ''}${point.completed ? ' completed-marker' : ''}`
      const icon = L.divIcon({
        className: 'route-map-marker-shell',
        html: `<span class="${markerClass}"><b>${point.label}</b>${point.completed ? '<i class="completion-check">✓</i>' : ''}${recommended ? '<i class="marker-label">추천</i>' : ''}${point.fragile ? '<i class="fragile-label">파손주의</i>' : ''}</span>`,
        iconSize: recommended ? [52, 54] : [38, 44],
        iconAnchor: recommended ? [26, 52] : [19, 42],
      })

      L.marker(coordinate, {
        icon,
        title: `${point.address}${point.fragile ? ' · 파손주의 상품 포함' : ''}`,
        alt: `${point.label} ${point.address}${point.completed ? ' 배송 완료' : ''}${point.fragile ? ' 파손주의 상품 포함' : ''}`,
        keyboard: true,
      }).addTo(routeLayer)
    })

    if (coordinates.length > 1) {
      points.slice(1).forEach((point, index) => {
        L.polyline(
          [coordinates[index], coordinates[index + 1]],
          point.completed
            ? {
                color: '#8e9991',
                weight: 5,
                opacity: 0.58,
              }
            : {
                color: '#1f6646',
                weight: 4,
                opacity: 0.72,
                dashArray: '8 10',
              },
        ).addTo(routeLayer)
      })

      map.fitBounds(L.latLngBounds(coordinates), {
        animate: true,
        duration: 0.45,
        maxZoom: 14,
        padding: [38, 38],
      })
      return
    }

    map.setView(coordinates[0], 14, { animate: true })
  }, [points, recommendedStopId])

  return (
    <section className="route-map-section" aria-label="배송 경로 지도">
      <div className="route-map-heading">
        <div>
          <span className="eyebrow">ROUTE MAP</span>
          <h2>배송지 지도</h2>
        </div>
        <div className="route-map-risk-legend" aria-label="위험도 핀 색상 안내">
          <span><i className="safe-dot" />안전</span>
          <span><i className="caution-dot" />주의</span>
          <span><i className="danger-dot" />위험</span>
          <span><i className="unknown-dot" />정보 없음</span>
          <span><i className="recommendation-dot" />추천</span>
        </div>
      </div>
      <div ref={containerRef} className="delivery-route-map" />
      <div className="route-map-legend" aria-label="지도 핀 목록">
        {points.map((point) => (
          <span
            key={point.key}
            className={`${point.completed ? 'completed-route-label' : ''}${Number(point.key) === recommendedStopId ? ' recommended-route-label' : ''}`.trim()}
          >
            <b className={point.departure ? 'departure-label' : ''}>{point.label}</b>
            {point.address}
            {point.completed && <em className="completed-label">완료</em>}
            {Number(point.key) === recommendedStopId && <em>다음 추천</em>}
            {point.fragile && <em className="fragile-route-label">파손주의</em>}
          </span>
        ))}
      </div>
    </section>
  )
}
