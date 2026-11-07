// Wrapper do fetch: toda chamada à API passa por aqui.
// - pega a base URL do .env.local (NEXT_PUBLIC_API_URL);
// - coloca o header Authorization quando há token;
// - transforma respostas de erro em ApiError (status, mensagem, campos);
// - trata 204 (sem corpo);
// - no 401 de quem estava logado, limpa a sessão e avisa o AuthContext.

import { montarQueryString, type ParametrosQuery } from "@/lib/queryString";
import type { CampoErro, ErroApi } from "@/types/api";

export const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/+$/, "");

/** Chaves do localStorage (o AuthContext usa as mesmas). */
export const CHAVE_TOKEN = "sportfy.token";
export const CHAVE_USUARIO = "sportfy.usuario";
/** Momento (em ms, Date.now()) em que o token vence. */
export const CHAVE_EXPIRA_EM = "sportfy.expiraEm";

/** Erro tipado lançado sempre que a API responde fora da faixa 2xx. */
export class ApiError extends Error {
  status: number;
  mensagem: string;
  campos: CampoErro[];

  constructor(status: number, mensagem: string, campos: CampoErro[] = []) {
    super(mensagem);
    this.name = "ApiError";
    this.status = status;
    this.mensagem = mensagem;
    this.campos = campos;
  }

  /** Mensagem do campo (para mostrar embaixo do input num 400). */
  mensagemDoCampo(campo: string): string | undefined {
    return this.campos.find((c) => c.campo === campo)?.mensagem;
  }

  /** Todos os erros de campo como objeto: { email: "...", senha: "..." }. */
  errosPorCampo(): Record<string, string> {
    return Object.fromEntries(this.campos.map((c) => [c.campo, c.mensagem]));
  }
}

// ---------- token ----------

export function obterToken(): string | null {
  // No servidor (SSR) não existe localStorage.
  if (typeof window === "undefined") return null;
  return localStorage.getItem(CHAVE_TOKEN);
}

export function limparSessao(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(CHAVE_TOKEN);
  localStorage.removeItem(CHAVE_USUARIO);
  localStorage.removeItem(CHAVE_EXPIRA_EM);
}

// O AuthContext registra aqui o que fazer quando a sessão expira (logout + ir para /login).
let aoNaoAutorizado: (() => void) | null = null;

export function definirAoNaoAutorizado(callback: (() => void) | null): void {
  aoNaoAutorizado = callback;
}

// ---------- requisição ----------

/** Sem isso, um servidor fora do ar (ex.: Tailscale desligado) deixa a tela travada por minutos. */
const TIMEOUT_PADRAO_MS = 20_000;

export interface OpcoesRequisicao {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  /** Objeto (vira JSON) ou FormData (upload). */
  body?: unknown;
  /** Parâmetros da query string. Valores vazios são ignorados. */
  query?: ParametrosQuery;
  /** Cancela a requisição depois desse tempo. Padrão 20 s; o chatbot usa 45000. */
  timeoutMs?: number;
  /** Para cancelar de fora (ex.: componente desmontado). */
  signal?: AbortSignal;
  /**
   * Não fazer logout automático num 401. Use em rotas onde 401 pode significar
   * "senha errada" (trocar senha, excluir conta), para a tela mostrar o erro.
   */
  ignorar401?: boolean;
}

export async function requisicao<T>(caminho: string, opcoes: OpcoesRequisicao = {}): Promise<T> {
  const {
    method = "GET",
    body,
    query,
    timeoutMs = TIMEOUT_PADRAO_MS,
    signal,
    ignorar401 = false,
  } = opcoes;

  const headers: Record<string, string> = { Accept: "application/json" };

  const token = obterToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  // FormData: NÃO definimos o Content-Type; o navegador coloca o boundary certo.
  let corpo: BodyInit | undefined;
  if (body instanceof FormData) {
    corpo = body;
  } else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    corpo = JSON.stringify(body);
  }

  // Timeout com AbortController, juntando com o signal de fora (se houver).
  const controle = new AbortController();
  let estourouTempo = false;
  const timer = timeoutMs
    ? setTimeout(() => {
        estourouTempo = true;
        controle.abort();
      }, timeoutMs)
    : undefined;
  const cancelarDeFora = () => controle.abort();
  if (signal?.aborted) controle.abort();
  signal?.addEventListener("abort", cancelarDeFora);

  let res: Response;
  try {
    res = await fetch(`${API_URL}${caminho}${montarQueryString(query)}`, {
      method,
      headers,
      body: corpo,
      signal: controle.signal,
    });
  } catch (erro) {
    if (estourouTempo) {
      throw new ApiError(408, "O servidor demorou demais para responder. Tente de novo.");
    }
    if (erro instanceof DOMException && erro.name === "AbortError") {
      throw erro; // cancelado de propósito: quem chamou decide o que fazer
    }
    throw new ApiError(0, "Não foi possível conectar ao servidor. Verifique sua internet.");
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", cancelarDeFora);
  }

  if (!res.ok) {
    const dados = await lerJson<ErroApi>(res);

    // 401 com token = sessão vencida. Sem token (ex.: senha errada no login) a tela trata.
    if (res.status === 401 && token && !ignorar401) {
      limparSessao();
      aoNaoAutorizado?.();
    }

    throw new ApiError(
      res.status,
      dados?.mensagem ?? mensagemPadrao(res.status),
      dados?.campos ?? [],
    );
  }

  // 204 No Content (ex.: cancelar partida, excluir conta).
  if (res.status === 204) return undefined as T;

  return (await lerJson<T>(res)) as T;
}

/** Lê o corpo como JSON; devolve undefined se vier vazio ou inválido. */
async function lerJson<T>(res: Response): Promise<T | undefined> {
  const texto = await res.text();
  if (!texto) return undefined;
  try {
    return JSON.parse(texto) as T;
  } catch {
    return undefined;
  }
}

function mensagemPadrao(status: number): string {
  switch (status) {
    case 400: return "Dados inválidos. Verifique os campos.";
    case 401: return "Sua sessão expirou. Entre novamente.";
    case 403: return "Você não tem permissão para fazer isso.";
    case 404: return "Não encontrado.";
    case 413: return "Arquivo maior que 5 MB.";
    case 429: return "Muitas tentativas. Aguarde um pouco.";
    default:
      return status >= 500
        ? "O servidor está com problemas. Tente mais tarde."
        : "Algo deu errado. Tente de novo.";
  }
}

/** Atalho para mostrar qualquer erro capturado num try/catch. */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) return erro.mensagem;
  return "Algo deu errado. Tente de novo.";
}

// Atalhos para os services ficarem curtos.
export const api = {
  get: <T>(caminho: string, query?: ParametrosQuery, opcoes?: OpcoesRequisicao) =>
    requisicao<T>(caminho, { ...opcoes, method: "GET", query }),
  post: <T>(caminho: string, body?: unknown, opcoes?: OpcoesRequisicao) =>
    requisicao<T>(caminho, { ...opcoes, method: "POST", body }),
  put: <T>(caminho: string, body?: unknown, opcoes?: OpcoesRequisicao) =>
    requisicao<T>(caminho, { ...opcoes, method: "PUT", body }),
  delete: <T = void>(caminho: string, opcoes?: OpcoesRequisicao) =>
    requisicao<T>(caminho, { ...opcoes, method: "DELETE" }),
};
