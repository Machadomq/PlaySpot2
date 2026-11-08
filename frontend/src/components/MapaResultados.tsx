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

export default function MapaResultados({ pontos, data }: { pontos: PontoDoMapa[]; data: string }) {
  const elemento = useRef<HTMLDivElement>(null)
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
      L.circleMarker([ponto.latitude, ponto.longitude], { radius: 10, color: '#12284c', fillOpacity: 0.8 })
        .bindTooltip(rotulo)
        .on('click', () => navegar(`/empresas/${ponto.id}?data=${data}`))
        .addTo(mapa)
    }
    mapa.fitBounds(L.latLngBounds(pontos.map((p) => [p.latitude, p.longitude])), { maxZoom: 15, padding: [24, 24] })

    return () => {
      mapa.remove()
    }
  }, [pontos, data, navegar])

  return <div ref={elemento} className="mapa" />
}
