import { SITUACOES } from "@/lib/rotulos";
import type { SituacaoPartida } from "@/types/partida";

/** Etiqueta colorida com a situação do evento (usa `situacao`, não `status`). */
export function SituacaoBadge({ situacao, className = "" }: { situacao: SituacaoPartida; className?: string }) {
  const { texto, classe } = SITUACOES[situacao] ?? SITUACOES.ABERTA;
  return (
    <span className={`inline-block rounded-full px-2.5 py-1 text-xs font-semibold ${classe} ${className}`}>
      {texto}
    </span>
  );
}
