import type { Quadra } from "@/types/local";
import { api } from "./api";

/** GET /api/quadras?localId=&esporteId= (público). Filtros opcionais. */
export function listarQuadras(filtros: { localId?: string; esporteId?: number } = {}) {
  return api.get<Quadra[]>("/api/quadras", filtros);
}
