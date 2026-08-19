import L from 'leaflet'
import { useEffect, useMemo, useRef } from 'react'
import 'leaflet/dist/leaflet.css'
import type { DeliveryStop } from '../types/api'

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
    },
    ...stops.map((stop, index) => ({
      key: String(stop.stopId),
      label: String(index + 1),
      address: stop.address,
      latitude: stop.latitude,
      longitude: stop.longitude,
      departure: false,
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
      const icon = L.divIcon({
        className: 'route-map-marker-shell',
        html: `<span class="route-map-marker${point.departure ? ' departure-marker' : ''}${recommended ? ' recommended-marker' : ''}"><b>${point.label}</b>${recommended ? '<i>추천</i>' : ''}</span>`,
        iconSize: recommended ? [52, 54] : [38, 44],
        iconAnchor: recommended ? [26, 52] : [19, 42],
      })

      L.marker(coordinate, {
        icon,
        title: point.address,
        alt: `${point.label} ${point.address}`,
        keyboard: true,
      }).addTo(routeLayer)
    })

    if (coordinates.length > 1) {
      L.polyline(coordinates, {
        color: '#1f6646',
        weight: 4,
        opacity: 0.72,
        dashArray: '8 10',
      }).addTo(routeLayer)

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
        <span>핀 번호는 현재 배송 순서입니다.</span>
      </div>
      <div ref={containerRef} className="delivery-route-map" />
      <div className="route-map-legend" aria-label="지도 핀 목록">
        {points.map((point) => (
          <span
            key={point.key}
            className={Number(point.key) === recommendedStopId ? 'recommended-route-label' : ''}
          >
            <b className={point.departure ? 'departure-label' : ''}>{point.label}</b>
            {point.address}
            {Number(point.key) === recommendedStopId && <em>다음 추천</em>}
          </span>
        ))}
      </div>
    </section>
  )
}
