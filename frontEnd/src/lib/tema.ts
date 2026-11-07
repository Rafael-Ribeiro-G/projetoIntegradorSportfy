// Modo claro / escuro.
// - A preferência ("claro", "escuro" ou "sistema") fica no localStorage.
// - O tema aplicado vai no atributo <html data-tema="claro|escuro">; o globals.css troca as cores por ele.
// - useTema() usa useSyncExternalStore: o React "assina" essas mudanças sem precisar de useEffect,
//   e no servidor (onde não existe localStorage) usa um valor padrão sem dar erro de hidratação.

"use client";

import { useSyncExternalStore } from "react";
import { CHAVE_TEMA as CHAVE, CONSULTA_ESCURO } from "./temaScript";

export type PreferenciaTema = "claro" | "escuro" | "sistema";
export type Tema = "claro" | "escuro";

const EVENTO = "sportfy:tema"; // avisa os componentes desta aba que a preferência mudou

export function lerPreferencia(): PreferenciaTema {
  try {
    const valor = localStorage.getItem(CHAVE);
    return valor === "claro" || valor === "escuro" ? valor : "sistema";
  } catch {
    return "sistema"; // navegação anônima pode bloquear o localStorage
  }
}

function temaDe(preferencia: PreferenciaTema): Tema {
  if (preferencia !== "sistema") return preferencia;
  return window.matchMedia(CONSULTA_ESCURO).matches ? "escuro" : "claro";
}

function aplicar(preferencia: PreferenciaTema) {
  document.documentElement.dataset.tema = temaDe(preferencia);
}

/** Salva a escolha, aplica na hora e avisa quem estiver mostrando o tema. */
export function definirTema(preferencia: PreferenciaTema) {
  try {
    if (preferencia === "sistema") localStorage.removeItem(CHAVE);
    else localStorage.setItem(CHAVE, preferencia);
  } catch {
    // sem localStorage: aplica só nesta visita
  }
  aplicar(preferencia);
  window.dispatchEvent(new Event(EVENTO));
}

function assinar(avisar: () => void) {
  const sistema = window.matchMedia(CONSULTA_ESCURO);
  // O sistema mudou (ex.: o celular entrou no modo escuro à noite): só importa se a escolha é "sistema".
  const aoMudarSistema = () => {
    if (lerPreferencia() === "sistema") aplicar("sistema");
    avisar();
  };
  // Outra aba do site mudou o tema.
  const aoMudarOutraAba = (e: StorageEvent) => {
    if (e.key !== CHAVE) return;
    aplicar(lerPreferencia());
    avisar();
  };
  window.addEventListener(EVENTO, avisar);
  window.addEventListener("storage", aoMudarOutraAba);
  sistema.addEventListener("change", aoMudarSistema);
  return () => {
    window.removeEventListener(EVENTO, avisar);
    window.removeEventListener("storage", aoMudarOutraAba);
    sistema.removeEventListener("change", aoMudarSistema);
  };
}

/** Use em Client Components: const { preferencia, tema, definirTema } = useTema(); */
export function useTema() {
  const preferencia = useSyncExternalStore(assinar, lerPreferencia, () => "sistema" as const);
  const tema = useSyncExternalStore(
    assinar,
    () => temaDe(lerPreferencia()),
    () => "claro" as const,
  );
  return { preferencia, tema, definirTema };
}
