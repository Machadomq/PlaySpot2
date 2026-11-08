import { useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useNavigate } from 'react-router-dom'

export interface PontoDoMapa {
  id: number
  nome: string
  latitude: number
  longitude: number
}

interface Props {
  pontos: PontoDoMapa[]
  data: string
  /** Empresa em evidência na lista: o ponto dela fica maior e em verde-limão. */
  destaque?: number | null
}

export default function MapaResultados({ pontos, data, destaque = null }: Props) {
  const elemento = useRef<HTMLDivElement>(null)
  const marcadores = useRef(new Map<number, L.CircleMarker>())
  const navegar = useNavigate()

  useEffect(() => {
    const mapa = L.map(elemento.current!)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap',
    }).addTo(mapa)

    for (const ponto of pontos) {
      // o Leaflet interpreta texto como HTML: o nome, escrito pelo dono, vai como conteúdo de um elemento
      const rotulo = document.createElement('span')
      rotulo.textContent = ponto.nome
      const marcador = L.circleMarker([ponto.latitude, ponto.longitude], { radius: 10, color: '#12284c', fillOpacity: 0.85 })
        .bindTooltip(rotulo)
        .on('click', () => navegar(`/empresas/${ponto.id}?data=${data}`))
        .addTo(mapa)
      marcadores.current.set(ponto.id, marcador)
    }
    mapa.fitBounds(L.latLngBounds(pontos.map((p) => [p.latitude, p.longitude])), { maxZoom: 15, padding: [24, 24] })

    const lista = marcadores.current
    return () => {
      mapa.remove()
      lista.clear()
    }
  }, [pontos, data, navegar])

  useEffect(() => {
    for (const [id, marcador] of marcadores.current) {
      const ativo = id === destaque
      marcador.setStyle({ fillColor: ativo ? '#d6f24a' : '#12284c', fillOpacity: ativo ? 1 : 0.85 })
      marcador.setRadius(ativo ? 14 : 10)
      if (ativo) marcador.bringToFront()
    }
  }, [destaque])

  return <div ref={elemento} className="mapa" />
}
