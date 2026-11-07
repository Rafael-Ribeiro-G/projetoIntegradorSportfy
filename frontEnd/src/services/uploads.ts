import type { UploadResponse } from "@/types/outros";
import { api } from "./api";

export const TIPOS_IMAGEM_ACEITOS = ["image/jpeg", "image/png", "image/webp"];
export const TAMANHO_MAXIMO_IMAGEM = 5 * 1024 * 1024; // 5 MB

/** Confere tipo e tamanho antes de enviar. Devolve a mensagem de erro, ou null se estiver ok. */
export function validarImagem(arquivo: File): string | null {
  if (!TIPOS_IMAGEM_ACEITOS.includes(arquivo.type)) {
    return "Envie uma imagem JPG, PNG ou WEBP.";
  }
  if (arquivo.size > TAMANHO_MAXIMO_IMAGEM) {
    return "A imagem precisa ter no máximo 5 MB.";
  }
  return null;
}

/** POST /api/uploads/imagens (logado). Use a `url` da resposta em fotoUrl/imagemUrl. */
export function enviarImagem(arquivo: File) {
  const form = new FormData();
  form.append("arquivo", arquivo);
  return api.post<UploadResponse>("/api/uploads/imagens", form);
}
