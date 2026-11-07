import type { NivelHabilidade, Usuario } from "./usuario";

/** POST /api/auth/login */
export interface LoginRequest {
  email: string;
  senha: string;
}

/** POST /api/auth/registro. Só nome, email e senha são obrigatórios. */
export interface RegistroRequest {
  nome: string;
  email: string;
  /** 8+ caracteres, com letra e número. */
  senha: string;
  telefone?: string;
  nivelHabilidade?: NivelHabilidade;
  fotoUrl?: string;
  bio?: string;
  esportesFavoritosIds?: number[];
}

/** Resposta do login (200) e do registro (201). */
export interface AuthResponse {
  token: string;
  tipo: string;
  expiraEmSegundos: number;
  usuario: Usuario;
}
