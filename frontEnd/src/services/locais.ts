import type { Local, LocalRequest, TipoLocal } from "@/types/local";
import { api } from "./api";

export interface FiltrosLocais {
  /** Busca no nome e no endereço. */
  texto?: string;
  tipo?: TipoLocal;
  /** Com latitude/longitude, a API ordena pelos mais próximos e traz `distanciaKm`. */
  latitude?: number;
  longitude?: number;
  raioKm?: number;
}

/** GET /api/locais (público). Locais privados só aparecem para o dono. */
export function listarLocais(filtros: FiltrosLocais = {}) {
  return api.get<Local[]>("/api/locais", filtros);
}

/** GET /api/locais/{id} (público): detalhe com a lista de quadras. */
export function buscarLocal(id: string) {
  return api.get<Local>(`/api/locais/${id}`);
}

/** POST /api/locais (logado): quem cadastra vira o dono. */
export function criarLocal(dados: LocalRequest) {
  return api.post<Local>("/api/locais", dados);
}
