import type { AuthResponse, LoginRequest, RegistroRequest } from "@/types/auth";
import { api } from "./api";

/** POST /api/auth/login (público) */
export function login(dados: LoginRequest) {
  return api.post<AuthResponse>("/api/auth/login", dados);
}

/** POST /api/auth/registro (público). A resposta 201 é igual à do login. */
export function registrar(dados: RegistroRequest) {
  return api.post<AuthResponse>("/api/auth/registro", dados);
}
