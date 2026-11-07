import type { ChatbotResponse } from "@/types/outros";
import { api } from "./api";

export const MAX_PERGUNTA = 1000;
/** A API desiste depois de 40 s; esperamos um pouco mais (45 s), como o API.md recomenda. */
const TIMEOUT_CHATBOT_MS = 45_000;

/** POST /api/chatbot/mensagem (logado) → { resposta } */
export function perguntarSportIA(pergunta: string, signal?: AbortSignal) {
  return api.post<ChatbotResponse>(
    "/api/chatbot/mensagem",
    { pergunta },
    { timeoutMs: TIMEOUT_CHATBOT_MS, signal },
  );
}
