import Link from "next/link";
import { SituacaoBadge } from "@/components/SituacaoBadge";
import { formatarDiaCurto, formatarHora, partesDoDia } from "@/lib/datas";
import { tituloDaPartida } from "@/lib/rotulos";
import type { Partida } from "@/types/partida";

/** Item compacto de evento (lista "meus eventos"): calendário + título + horário + local. */
export function EventoItemLista({ partida, organizador = false }: { partida: Partida; organizador?: boolean }) {
  const { dia, mes } = partesDoDia(partida.dataHoraInicio);

  return (
    <Link
      href={`/eventos/${partida.id}`}
      className="flex items-center gap-4 rounded-2xl bg-superficie p-3 shadow-sm ring-1 ring-borda transition hover:shadow-md hover:ring-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
    >
      {/* "Folhinha" com dia e mês */}
      <div
        aria-hidden
        className="flex h-14 w-14 shrink-0 flex-col items-center justify-center rounded-xl bg-primary-light text-primary-dark"
      >
        <span className="text-xl font-bold leading-none">{dia}</span>
        <span className="text-xs font-semibold uppercase">{mes}</span>
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate font-semibold">{tituloDaPartida(partida)}</p>
        <p className="truncate text-sm text-texto-suave">
          <span className="sr-only">{formatarDiaCurto(partida.dataHoraInicio)}, </span>
          {formatarHora(partida.dataHoraInicio)} · {partida.esporte.nome} · {partida.local.nome}
        </p>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-1">
        {partida.situacao !== "ABERTA" && <SituacaoBadge situacao={partida.situacao} />}
        {organizador && (
          <span className="rounded-full bg-secondary/15 px-2 py-0.5 text-xs font-semibold text-secondary">
            Organizador
          </span>
        )}
      </div>
    </Link>
  );
}
