import type { Esporte } from "@/types/esporte";
import { api } from "./api";

/** GET /api/esportes (público) */
export function listarEsportes() {
  return api.get<Esporte[]>("/api/esportes");
}

/** GET /api/esportes/{id} (público) */
export function buscarEsporte(id: number) {
  return api.get<Esporte>(`/api/esportes/${id}`);
}
