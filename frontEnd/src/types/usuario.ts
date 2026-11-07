import type { Esporte } from "./esporte";

/** Nível de habilidade: 1 (iniciante) a 5 (avançado). */
export type NivelHabilidade = 1 | 2 | 3 | 4 | 5;

/** Usuário logado (GET /api/usuarios/me e resposta do login). */
export interface Usuario {
  id: string;
  nome: string;
  email: string;
  telefone?: string;
  nivelHabilidade?: NivelHabilidade;
  fotoUrl?: string;
  bio?: string;
  esportesFavoritos?: Esporte[];
  criadoEm?: string;
}

/** Perfil público (GET /api/usuarios/{id}): sem e-mail e telefone. */
export interface UsuarioPublico {
  id: string;
  nome: string;
  nivelHabilidade?: NivelHabilidade;
  fotoUrl?: string;
  bio?: string;
  esportesFavoritos?: Esporte[];
}

/** Versão resumida que aparece dentro de outros objetos (criador, dono). */
export interface UsuarioResumo {
  id: string;
  nome: string;
  fotoUrl?: string;
}

/**
 * PUT /api/usuarios/me.
 * Atenção: campo enviado como null é APAGADO, então mande sempre todos os valores atuais.
 */
export interface AtualizarUsuarioRequest {
  nome: string;
  telefone: string | null;
  nivelHabilidade: NivelHabilidade | null;
  fotoUrl: string | null;
  bio: string | null;
  /** Substitui a lista atual. Sem o campo, os favoritos ficam como estão. */
  esportesFavoritosIds?: number[];
}

/** PUT /api/usuarios/me/senha (devolve um token novo). */
export interface TrocarSenhaRequest {
  senhaAtual: string;
  novaSenha: string;
}

/** POST /api/usuarios/me/exclusao (LGPD). */
export interface ExcluirContaRequest {
  senha: string;
}
