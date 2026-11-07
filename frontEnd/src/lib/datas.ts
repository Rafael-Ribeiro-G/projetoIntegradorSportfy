// Datas da API vêm SEM fuso, já no horário de Brasília: "2026-10-25T19:00:00".
// Para exibir sem que o navegador "corrija" o horário, montamos a data em UTC e
// formatamos também em UTC: os números saem exatamente como vieram.
// Nunca use toISOString() para mandar datas (ele converte para UTC e muda a hora).

/** "2026-10-25T19:00:00" → Date "congelado" (em UTC) só para formatar. */
function paraDataUtc(iso: string): Date {
  const [data, hora = "00:00:00"] = iso.split("T");
  const [ano, mes, dia] = data.split("-").map(Number);
  const [h = 0, min = 0, s = 0] = hora.split(":").map(Number);
  return new Date(Date.UTC(ano, mes - 1, dia, h, min, s));
}

const fmtDiaCurto = new Intl.DateTimeFormat("pt-BR", {
  timeZone: "UTC",
  weekday: "short",
  day: "2-digit",
  month: "short",
});

const fmtDiaLongo = new Intl.DateTimeFormat("pt-BR", {
  timeZone: "UTC",
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
});

const fmtHora = new Intl.DateTimeFormat("pt-BR", {
  timeZone: "UTC",
  hour: "2-digit",
  minute: "2-digit",
});

const fmtMes = new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC", month: "short" });

/** Para o "calendário" do item de lista: { dia: "25", mes: "out" } */
export function partesDoDia(iso: string): { dia: string; mes: string } {
  const d = paraDataUtc(iso);
  return {
    dia: String(d.getUTCDate()),
    mes: fmtMes.format(d).replace(".", ""),
  };
}

/** "sáb., 25 de out." */
export function formatarDiaCurto(iso: string): string {
  return fmtDiaCurto.format(paraDataUtc(iso));
}

/** "sábado, 25 de outubro de 2026" */
export function formatarDiaLongo(iso: string): string {
  return fmtDiaLongo.format(paraDataUtc(iso));
}

/** "19:00" */
export function formatarHora(iso: string): string {
  return fmtHora.format(paraDataUtc(iso));
}

/** "sáb., 25 de out. · 19:00–20:30" (mostra o dia do fim se for outro dia). */
export function formatarPeriodo(inicio: string, fim: string): string {
  const mesmoDia = inicio.slice(0, 10) === fim.slice(0, 10);
  const textoFim = mesmoDia ? formatarHora(fim) : `${formatarDiaCurto(fim)} ${formatarHora(fim)}`;
  return `${formatarDiaCurto(inicio)} · ${formatarHora(inicio)}–${textoFim}`;
}

/** 90 → "1h30", 60 → "1h", 45 → "45 min" */
export function formatarDuracao(minutos: number): string {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, "0")}`;
}

/** Valor de <input type="date"> ("2026-10-25") → "2026-10-25T00:00:00" */
export function inicioDoDia(data: string): string {
  return `${data}T00:00:00`;
}

/** Valor de <input type="date"> ("2026-10-25") → "2026-10-25T23:59:59" */
export function fimDoDia(data: string): string {
  return `${data}T23:59:59`;
}

/** Valor de <input type="datetime-local"> ("2026-10-25T19:00") → "2026-10-25T19:00:00" */
export function datetimeLocalParaApi(valor: string): string {
  return valor.length === 16 ? `${valor}:00` : valor;
}

/** Data da API ("2026-10-25T19:00:00") → valor do datetime-local ("2026-10-25T19:00") */
export function apiParaDatetimeLocal(iso: string): string {
  return iso.slice(0, 16);
}

/** Minutos entre duas datas no formato da API ou do datetime-local. */
export function minutosEntre(inicio: string, fim: string): number {
  return (paraDataUtc(fim).getTime() - paraDataUtc(inicio).getTime()) / 60_000;
}

const dois = (n: number) => String(n).padStart(2, "0");

/** "2026-10-25T19:00" + 90 → "2026-10-25T20:30" (sem mexer com fuso). */
export function somarMinutos(valor: string, minutos: number): string {
  const d = new Date(paraDataUtc(valor).getTime() + minutos * 60_000);
  return `${d.getUTCFullYear()}-${dois(d.getUTCMonth() + 1)}-${dois(d.getUTCDate())}T${dois(d.getUTCHours())}:${dois(d.getUTCMinutes())}`;
}

/** Agora, no relógio do computador, no formato do datetime-local ("2026-11-07T14:05"). */
export function agoraDatetimeLocal(): string {
  const d = new Date();
  return `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}T${dois(d.getHours())}:${dois(d.getMinutes())}`;
}
