export interface Posicao {
  latitude: number;
  longitude: number;
}

/**
 * Pede a localização ao navegador. Devolve null se a pessoa negar,
 * se o navegador não suportar ou se demorar demais. Nunca lança erro.
 */
export function obterPosicao(timeoutMs = 10_000): Promise<Posicao | null> {
  return new Promise((resolve) => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      resolve(null);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ latitude: p.coords.latitude, longitude: p.coords.longitude }),
      () => resolve(null),
      { timeout: timeoutMs, maximumAge: 5 * 60 * 1000 }, // aceita posição de até 5 min atrás
    );
  });
}
