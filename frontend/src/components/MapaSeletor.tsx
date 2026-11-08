import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const CENTRO_DO_BRASIL: L.LatLngTuple = [-14.2, -51.9]

interface Props {
  latitude: number | null
  longitude: number | null
  /** Região a mostrar sem marcar ponto. */
  centro?: { latitude: number; longitude: number } | null
  aoMudar: (latitude: number, longitude: number) => void
}

/** O dono clica no mapa para marcar a posição da empresa. */
export default function MapaSeletor({ latitude, longitude, centro, aoMudar }: Props) {
  const elemento = useRef<HTMLDivElement>(null)
  const mapa = useRef<L.Map | null>(null)
  const marcador = useRef<L.CircleMarker | null>(null)
  const retorno = useRef(aoMudar)

  useEffect(() => {
    retorno.current = aoMudar
  }, [aoMudar])

  useEffect(() => {
    const instancia = L.map(elemento.current!).setView(CENTRO_DO_BRASIL, 4)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
    }).addTo(instancia)
    instancia.on('click', (evento) => {
      retorno.current(Number(evento.latlng.lat.toFixed(6)), Number(evento.latlng.lng.toFixed(6)))
    })
    mapa.current = instancia
    return () => {
      instancia.remove()
      mapa.current = null
      marcador.current = null
    }
  }, [])

  useEffect(() => {
    if (!mapa.current || latitude === null || longitude === null) return
    const posicao: L.LatLngTuple = [latitude, longitude]
    if (marcador.current) {
      marcador.current.setLatLng(posicao)
    } else {
      marcador.current = L.circleMarker(posicao, { radius: 10, color: '#12284c', fillOpacity: 0.8 }).addTo(mapa.current)
      mapa.current.setView(posicao, 16)
    }
    if (!mapa.current.getBounds().contains(posicao)) mapa.current.setView(posicao, 16)
  }, [latitude, longitude])

  useEffect(() => {
    if (mapa.current && centro) mapa.current.setView([centro.latitude, centro.longitude], 14)
  }, [centro])

  return <div ref={elemento} className="mapa" />
}
