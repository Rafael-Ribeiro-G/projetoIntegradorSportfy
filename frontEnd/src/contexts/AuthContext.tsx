"use client";

// Guarda quem está logado (token + usuário) e oferece login(), registrar() e logout()
// para qualquer tela. Os dados ficam no localStorage para sobreviver ao F5.

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  CHAVE_EXPIRA_EM,
  CHAVE_TOKEN,
  CHAVE_USUARIO,
  definirAoNaoAutorizado,
  limparSessao,
} from "@/services/api";
import * as authService from "@/services/auth";
import type { AuthResponse, LoginRequest, RegistroRequest } from "@/types/auth";
import type { Usuario } from "@/types/usuario";

interface AuthContextValor {
  token: string | null;
  usuario: Usuario | null;
  /** true enquanto ainda estamos lendo o localStorage (evita "piscar" a tela de login). */
  carregando: boolean;
  login: (dados: LoginRequest) => Promise<Usuario>;
  registrar: (dados: RegistroRequest) => Promise<Usuario>;
  logout: () => void;
  /** Depois de editar o perfil, para a tela toda ver o usuário novo. */
  atualizarUsuario: (usuario: Usuario) => void;
  /** Para respostas que trazem um token novo (ex.: troca de senha). */
  salvarSessao: (resposta: AuthResponse) => void;
}

const AuthContext = createContext<AuthContextValor | null>(null);

/** Lê a sessão salva. Se o token já venceu, apaga e devolve null. */
function lerSessaoSalva(): { token: string; usuario: Usuario } | null {
  const token = localStorage.getItem(CHAVE_TOKEN);
  const usuarioJson = localStorage.getItem(CHAVE_USUARIO);
  const expiraEm = Number(localStorage.getItem(CHAVE_EXPIRA_EM) ?? 0);

  if (!token || !usuarioJson || Date.now() >= expiraEm) {
    limparSessao();
    return null;
  }
  try {
    return { token, usuario: JSON.parse(usuarioJson) as Usuario };
  } catch {
    limparSessao();
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);

  // O localStorage só existe no navegador, por isso a leitura fica num useEffect.
  useEffect(() => {
    const sessao = lerSessaoSalva();
    /* eslint-disable react-hooks/set-state-in-effect -- sincroniza com o localStorage uma única vez, ao abrir */
    setToken(sessao?.token ?? null);
    setUsuario(sessao?.usuario ?? null);
    setCarregando(false);
    /* eslint-enable react-hooks/set-state-in-effect */
  }, []);

  const logout = useCallback(() => {
    limparSessao();
    setToken(null);
    setUsuario(null);
    router.replace("/login");
  }, [router]);

  // Quando a API responder 401 com token (sessão vencida), o api.ts chama isto.
  useEffect(() => {
    definirAoNaoAutorizado(() => {
      setToken(null);
      setUsuario(null);
      router.replace("/login");
    });
    return () => definirAoNaoAutorizado(null);
  }, [router]);

  const salvarSessao = useCallback((resposta: AuthResponse) => {
    localStorage.setItem(CHAVE_TOKEN, resposta.token);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(resposta.usuario));
    localStorage.setItem(
      CHAVE_EXPIRA_EM,
      String(Date.now() + resposta.expiraEmSegundos * 1000),
    );
    setToken(resposta.token);
    setUsuario(resposta.usuario);
  }, []);

  const login = useCallback(
    async (dados: LoginRequest) => {
      const resposta = await authService.login(dados);
      salvarSessao(resposta);
      return resposta.usuario;
    },
    [salvarSessao],
  );

  const registrar = useCallback(
    async (dados: RegistroRequest) => {
      const resposta = await authService.registrar(dados);
      salvarSessao(resposta);
      return resposta.usuario;
    },
    [salvarSessao],
  );

  const atualizarUsuario = useCallback((novo: Usuario) => {
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(novo));
    setUsuario(novo);
  }, []);

  const valor = useMemo(
    () => ({ token, usuario, carregando, login, registrar, logout, atualizarUsuario, salvarSessao }),
    [token, usuario, carregando, login, registrar, logout, atualizarUsuario, salvarSessao],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

/** Use dentro de qualquer Client Component: const { usuario, logout } = useAuth(); */
export function useAuth(): AuthContextValor {
  const contexto = useContext(AuthContext);
  if (!contexto) throw new Error("useAuth precisa estar dentro do <AuthProvider>.");
  return contexto;
}
