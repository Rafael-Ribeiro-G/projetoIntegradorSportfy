import Link from "next/link";
import { ImagemCapa } from "@/components/ImagemCapa";
import { SituacaoBadge } from "@/components/SituacaoBadge";
import { formatarPeriodo } from "@/lib/datas";
import { formatarDistancia, textoVagas, tituloDaPartida } from "@/lib/rotulos";
import type { Partida } from "@/types/partida";

/** Card de um evento na lista (clicar abre o detalhe). */
export function EventoCard({ partida }: { partida: Partida }) {
  const titulo = tituloDaPartida(partida);

  return (
    <Link
      href={`/eventos/${partida.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl bg-superficie shadow-sm ring-1 ring-borda transition hover:-translate-y-0.5 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
    >
      <div className="relative aspect-video">
        <ImagemCapa src={partida.imagemUrl} alt={`Capa: ${titulo}`} esporte={partida.esporte.nome} />
        <SituacaoBadge situacao={partida.situacao} className="absolute left-3 top-3 shadow-sm" />
        {partida.distanciaKm !== undefined && (
          <span className="absolute right-3 top-3 rounded-full bg-black/60 px-2.5 py-1 text-xs font-medium text-white">
            a {formatarDistancia(partida.distanciaKm)}
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <span className="text-xs font-semibold uppercase tracking-wide text-primary">
          {partida.esporte.nome}
        </span>
        <h3 className="line-clamp-2 text-lg font-bold leading-snug group-hover:text-primary">
          {titulo}
        </h3>
        <p className="text-sm text-texto-suave">
          <span aria-hidden>📅 </span>
          {formatarPeriodo(partida.dataHoraInicio, partida.dataHoraFim)}
        </p>
        <p className="line-clamp-1 text-sm text-texto-suave">
          <span aria-hidden>📍 </span>
          {partida.local.nome}
        </p>

        <div className="mt-auto flex items-center justify-between border-t border-borda pt-3 text-sm">
          <span className="text-texto-suave">
            {partida.participantesConfirmados ?? 0} confirmado
            {(partida.participantesConfirmados ?? 0) === 1 ? "" : "s"}
          </span>
          <span className="font-semibold">{textoVagas(partida)}</span>
        </div>
      </div>
    </Link>
  );
}

/** "Esqueleto" cinza do card, mostrado enquanto carrega. */
export function EventoCardEsqueleto() {
  return (
    <div aria-hidden className="flex animate-pulse flex-col overflow-hidden rounded-2xl bg-superficie ring-1 ring-borda">
      <div className="aspect-video bg-borda" />
      <div className="flex flex-col gap-3 p-4">
        <div className="h-3 w-1/4 rounded bg-borda" />
        <div className="h-5 w-3/4 rounded bg-borda" />
        <div className="h-3 w-2/3 rounded bg-borda" />
        <div className="h-3 w-1/2 rounded bg-borda" />
      </div>
    </div>
  );
}
