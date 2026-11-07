"use client";

import { useState } from "react";

/**
 * Capa do evento. Sem imagem (ou se o link quebrar), mostra um fundo verde com o nome do esporte.
 * Usamos <img> simples porque as imagens vêm de vários servidores (API e links externos).
 */
export function ImagemCapa({
  src,
  alt,
  esporte,
  className = "",
}: {
  src?: string;
  alt: string;
  esporte: string;
  className?: string;
}) {
  const [quebrou, setQuebrou] = useState(false);

  if (src && !quebrou) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- imagens de origens variadas (API/links externos)
      <img
        src={src}
        alt={alt}
        loading="lazy"
        onError={() => setQuebrou(true)}
        className={`h-full w-full object-cover ${className}`}
      />
    );
  }

  return (
    <div
      role="img"
      aria-label={alt}
      className={`flex h-full w-full items-center justify-center bg-gradient-to-br from-primary to-primary-dark ${className}`}
    >
      <span className="px-4 text-center text-2xl font-extrabold tracking-tight text-white/90">
        {esporte}
      </span>
    </div>
  );
}
