import type { AuthResponse } from "@/types/auth";
import type { MinhasPartidas } from "@/types/partida";
import type {
  AtualizarUsuarioRequest,
  ExcluirContaRequest,
  TrocarSenhaRequest,
  Usuario,
  UsuarioPublico,
} from "@/types/usuario";
import { api } from "./api";

/** GET /api/usuarios/me */
export function buscarMeuUsuario() {
  return api.get<Usuario>("/api/usuarios/me");
}

/** PUT /api/usuarios/me. Campo null é apagado: mande sempre todos os valores atuais. */
export function atualizarMeuUsuario(dados: AtualizarUsuarioRequest) {
  return api.put<Usuario>("/api/usuarios/me", dados);
}

/**
 * PUT /api/usuarios/me/senha. Devolve um token novo (o antigo para de valer).
 * ignorar401: senha atual errada não deve deslogar a pessoa.
 */
export function trocarSenha(dados: TrocarSenhaRequest) {
  return api.put<Partial<AuthResponse> & { token: string }>("/api/usuarios/me/senha", dados, {
    ignorar401: true,
  });
}

/** GET /api/usuarios/me/partidas */
export function buscarMinhasPartidas() {
  return api.get<MinhasPartidas>("/api/usuarios/me/partidas");
}

/** GET /api/usuarios/me/dados (LGPD: exporta tudo). O formato é livre. */
export function exportarMeusDados() {
  return api.get<unknown>("/api/usuarios/me/dados");
}

/** POST /api/usuarios/me/exclusao (LGPD) → 204 */
export function excluirMinhaConta(dados: ExcluirContaRequest) {
  return api.post<void>("/api/usuarios/me/exclusao", dados, { ignorar401: true });
}

/** GET /api/usuarios/{id} (perfil público) */
export function buscarUsuarioPublico(id: string) {
  return api.get<UsuarioPublico>(`/api/usuarios/${id}`);
}
