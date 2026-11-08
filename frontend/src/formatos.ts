export const ANTECEDENCIA_MAXIMA_DIAS = 60

export const formatarPreco = (valor: number) =>
  valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

const paraDataIso = (data: Date) => data.toLocaleDateString('en-CA')

export const hoje = () => paraDataIso(new Date())

export function dataMaxima() {
  const data = new Date()
  data.setDate(data.getDate() + ANTECEDENCIA_MAXIMA_DIAS)
  return paraDataIso(data)
}

export const formatarDataHora = (instante: string, fuso: string) =>
  new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: fuso }).format(
    new Date(instante),
  )
