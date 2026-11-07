/** GET /api/esportes. Único recurso com id numérico. */
export interface Esporte {
  id: number;
  nome: string;
  quantidadeJogadoresPadrao?: number;
}
