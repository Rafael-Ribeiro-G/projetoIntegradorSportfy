import type { MinhasPartidas, Partida } from "@/types/partida";

export interface MeuEvento {
  partida: Partida;
  /** true se fui eu que criei. */
  organizador: boolean;
}

/**
 * Junta "criadas" e "participando" sem repetir (quem cria também participa)
 * e ordena pela data de início (as comparações de texto funcionam porque o formato é ISO).
 */
export function juntarMeusEventos({ criadas, participando }: MinhasPartidas): MeuEvento[] {
  const porId = new Map<string, MeuEvento>();
  for (const p of participando) porId.set(p.id, { partida: p, organizador: false });
  for (const p of criadas) porId.set(p.id, { partida: p, organizador: true });
  return [...porId.values()].sort((a, b) =>
    a.partida.dataHoraInicio.localeCompare(b.partida.dataHoraInicio),
  );
}

/** Só os que ainda vão acontecer ou estão acontecendo (sem encerrados e cancelados). */
export function apenasProximos(eventos: MeuEvento[]): MeuEvento[] {
  return eventos.filter(
    (e) => e.partida.situacao !== "ENCERRADA" && e.partida.situacao !== "CANCELADA",
  );
}
