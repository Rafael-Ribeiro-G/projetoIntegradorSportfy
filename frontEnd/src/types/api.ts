// Tipos genéricos usados por várias rotas da API.

/** Um campo inválido devolvido num erro 400. */
export interface CampoErro {
  campo: string;
  mensagem: string;
}

/** Formato único de erro da API (ver "Erros" no API.md). */
export interface ErroApi {
  timestamp?: string;
  status: number;
  erro?: string;
  mensagem?: string;
  caminho?: string;
  campos?: CampoErro[];
}

/** Resposta paginada (ex.: GET /api/partidas). */
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}
