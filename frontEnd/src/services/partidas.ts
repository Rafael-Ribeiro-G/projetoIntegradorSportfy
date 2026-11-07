// Rotas de partidas (= eventos). Usamos sempre /api/partidas (o /api/eventos é só um apelido).

import type { Pagina } from "@/types/api";
import type {
  FiltrosPartidas,
  FiltrosSugestoes,
  Participante,
  Partida,
  PartidaRequest,
} from "@/types/partida";
import { api } from "./api";

/** GET /api/partidas (público). Paginado: começa na página 0. */
export function listarPartidas(filtros: FiltrosPartidas = {}) {
  return api.get<Pagina<Partida>>("/api/partidas", filtros);
}

/** GET /api/partidas/sugestoes (logado). Eventos com vaga em que o usuário ainda não está. */
export function listarSugestoes(filtros: FiltrosSugestoes = {}) {
  return api.get<Partida[]>("/api/partidas/sugestoes", filtros);
}

/** GET /api/partidas/{id} (público) */
export function buscarPartida(id: string) {
  return api.get<Partida>(`/api/partidas/${id}`);
}

/** POST /api/partidas (logado). Quem cria já entra como participante. */
export function criarPartida(dados: PartidaRequest) {
  return api.post<Partida>("/api/partidas", dados);
}

/** PUT /api/partidas/{id} (só quem criou, antes de começar) */
export function atualizarPartida(id: string, dados: PartidaRequest) {
  return api.put<Partida>(`/api/partidas/${id}`, dados);
}

/** DELETE /api/partidas/{id} (só quem criou): cancela o evento → 204 */
export function cancelarPartida(id: string) {
  return api.delete(`/api/partidas/${id}`);
}

/** POST /api/partidas/{id}/participacao: entra (409 se já está, 422 se lotada/cancelada/começou) */
export function participarDaPartida(id: string) {
  return api.post<void>(`/api/partidas/${id}/participacao`);
}

/** DELETE /api/partidas/{id}/participacao: sai (o criador não pode sair, deve cancelar) */
export function sairDaPartida(id: string) {
  return api.delete(`/api/partidas/${id}/participacao`);
}

/** GET /api/partidas/{id}/participantes (logado) */
export function listarParticipantes(id: string) {
  return api.get<Participante[]>(`/api/partidas/${id}/participantes`);
}
