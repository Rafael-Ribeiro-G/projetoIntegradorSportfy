/** POST /api/uploads/imagens (201) */
export interface UploadResponse {
  url: string;
  nome: string;
  tipo: string;
  tamanhoBytes: number;
}

/** POST /api/chatbot/mensagem */
export interface ChatbotRequest {
  pergunta: string;
}

export interface ChatbotResponse {
  resposta: string;
}
