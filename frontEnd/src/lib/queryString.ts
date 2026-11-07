/**
 * Parâmetros de URL: qualquer objeto (ex.: FiltrosPartidas).
 * Valores `undefined`, `null` e "" são ignorados.
 */
export type ParametrosQuery = object;

/**
 * Monta "?a=1&b=2" a partir de um objeto, pulando os valores vazios.
 * Ex.: montarQueryString({ esporteId: 2, texto: "" }) → "?esporteId=2"
 */
export function montarQueryString(params?: ParametrosQuery): string {
  if (!params) return "";

  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(params)) {
    if (valor === undefined || valor === null || valor === "") continue;
    busca.append(chave, String(valor));
  }

  const texto = busca.toString();
  return texto ? `?${texto}` : "";
}
