"use client";

import { useState } from "react";

const TAMANHOS = {
  pequeno: "h-10 w-10 text-sm",
  medio: "h-14 w-14 text-lg",
  grande: "h-24 w-24 text-3xl",
};

/** "Ana Souza" → "AS" */
function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  const primeira = partes[0]?.[0] ?? "";
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (primeira + ultima).toUpperCase();
}

/** Foto redonda da pessoa; sem foto (ou se quebrar), mostra as iniciais. */
export function Avatar({
  nome,
  fotoUrl,
  tamanho = "pequeno",
}: {
  nome: string;
  fotoUrl?: string;
  tamanho?: keyof typeof TAMANHOS;
}) {
  const [quebrou, setQuebrou] = useState(false);
  const classes = `${TAMANHOS[tamanho]} shrink-0 overflow-hidden rounded-full`;

  if (fotoUrl && !quebrou) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- fotos vêm do servidor da API
      <img
        src={fotoUrl}
        alt={`Foto de ${nome}`}
        onError={() => setQuebrou(true)}
        className={`${classes} object-cover`}
      />
    );
  }

  return (
    <span
      aria-hidden
      className={`${classes} flex items-center justify-center bg-primary-light font-bold text-primary-forte`}
    >
      {iniciais(nome)}
    </span>
  );
}
