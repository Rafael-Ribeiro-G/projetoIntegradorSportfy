import type { Esporte } from "./esporte";
import type { UsuarioResumo } from "./usuario";

export type TipoLocal = "QUADRA" | "PARQUE" | "ACADEMIA" | "PISTA" | "RUA" | "OUTRO";

/** Quadra como aparece dentro de um local ou de uma partida. */
export interface QuadraResumo {
  id: string;
  nome: string;
  precoHora?: number;
  esporte?: Esporte;
}

/** Local resumido, dentro de uma partida. */
export interface LocalResumo {
  id: string;
  nome: string;
  endereco?: string;
  tipo?: TipoLocal;
  latitude?: number;
  longitude?: number;
}

/** GET /api/locais e GET /api/locais/{id}. */
export interface Local extends LocalResumo {
  publico?: boolean;
  dono?: UsuarioResumo;
  quadras?: QuadraResumo[];
  /** Só vem quando a busca manda latitude/longitude. */
  distanciaKm?: number;
}

/** POST/PUT /api/locais. Latitude e longitude vão juntas. */
export interface LocalRequest {
  nome: string;
  endereco?: string;
  latitude?: number;
  longitude?: number;
  tipo?: TipoLocal;
  publico?: boolean;
}

/** GET /api/quadras e GET /api/quadras/{id}. */
export interface Quadra extends QuadraResumo {
  local?: LocalResumo;
}

/** POST/PUT /api/quadras */
export interface QuadraRequest {
  localId: string;
  esporteId: number;
  nome: string;
  precoHora?: number;
}
