// Textos e cores para mostrar os valores da API na tela.

import type { TipoLocal } from "@/types/local";
import type { Partida, SituacaoPartida } from "@/types/partida";
import type { NivelHabilidade } from "@/types/usuario";

export const SITUACOES: Record<SituacaoPartida, { texto: string; classe: string }> = {
  ABERTA: { texto: "Aberta", classe: "bg-primary-light text-primary-dark" },
  LOTADA: { texto: "Lotada", classe: "bg-orange-100 text-orange-700" },
  EM_ANDAMENTO: { texto: "Em andamento", classe: "bg-sky-100 text-sky-700" },
  ENCERRADA: { texto: "Encerrada", classe: "bg-gray-200 text-gray-700" },
  CANCELADA: { texto: "Cancelada", classe: "bg-red-100 text-red-700" },
};

export const TIPOS_LOCAL: Record<TipoLocal, string> = {
  QUADRA: "Quadra",
  PARQUE: "Parque",
  ACADEMIA: "Academia",
  PISTA: "Pista",
  RUA: "Rua",
  OUTRO: "Outro",
};

export const NIVEIS: { valor: NivelHabilidade; texto: string }[] = [
  { valor: 1, texto: "1 - Iniciante" },
  { valor: 2, texto: "2 - Básico" },
  { valor: 3, texto: "3 - Intermediário" },
  { valor: 4, texto: "4 - Avançado" },
  { valor: 5, texto: "5 - Profissional" },
];

export function textoNivel(nivel?: NivelHabilidade): string | undefined {
  return NIVEIS.find((n) => n.valor === nivel)?.texto;
}

/** Título do evento; se não tiver, "Futsal · Parque Ibirapuera". */
export function tituloDaPartida(p: Partida): string {
  return p.titulo?.trim() || `${p.esporte.nome} · ${p.local.nome}`;
}

/** `vagasRestantes` não vem quando o evento não tem limite. */
export function textoVagas(p: Partida): string {
  if (p.vagasRestantes === undefined) return "Sem limite de vagas";
  if (p.vagasRestantes <= 0) return "Sem vagas";
  return p.vagasRestantes === 1 ? "1 vaga" : `${p.vagasRestantes} vagas`;
}

/** 0.35 → "350 m", 4.2 → "4,2 km" */
export function formatarDistancia(km: number): string {
  if (km < 1) return `${Math.round(km * 1000)} m`;
  return `${km.toLocaleString("pt-BR", { maximumFractionDigits: 1 })} km`;
}

/** 80.5 → "R$ 80,50" */
export function formatarPreco(valor: number): string {
  return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}
