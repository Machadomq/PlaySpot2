export const ANTECEDENCIA_MAXIMA_DIAS = 60

export const formatarPreco = (valor: number) =>
  valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

const paraDataIso = (data: Date) => data.toLocaleDateString('en-CA')

export const hoje = () => paraDataIso(new Date())

// meio-dia evita que mudanças de fuso troquem o dia
const paraDate = (data: string) => new Date(`${data}T12:00:00`)

export function somarDias(data: string, dias: number) {
  const resultado = paraDate(data)
  resultado.setDate(resultado.getDate() + dias)
  return paraDataIso(resultado)
}

export function segundaDaSemana(data: string) {
  return somarDias(data, -((paraDate(data).getDay() + 6) % 7))
}

export const primeiroDiaDoMes = (data: string) => `${data.slice(0, 7)}-01`

export function ultimoDiaDoMes(data: string) {
  const dia = paraDate(primeiroDiaDoMes(data))
  return paraDataIso(new Date(dia.getFullYear(), dia.getMonth() + 1, 0))
}

export const formatarDia = (data: string) =>
  paraDate(data).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: '2-digit' })

export function dataMaxima() {
  const data = new Date()
  data.setDate(data.getDate() + ANTECEDENCIA_MAXIMA_DIAS)
  return paraDataIso(data)
}

/** Dia, mês e hora separados, para compor o canhoto do bilhete de reserva. */
export function partesDoHorario(instante: string, fuso: string) {
  const data = new Date(instante)
  const parte = (opcoes: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat('pt-BR', { ...opcoes, timeZone: fuso }).format(data)
  return {
    dia: parte({ day: '2-digit' }),
    mes: parte({ month: 'short' }).replace('.', ''),
    hora: parte({ hour: '2-digit', minute: '2-digit' }),
  }
}

export const formatarDataHora =(instante: string, fuso: string) =>
  new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: fuso }).format(
    new Date(instante),
  )
