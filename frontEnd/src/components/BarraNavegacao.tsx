"use client";

// Cabeçalho de navegação do site, fixo no topo.
// - Computador (md+): logo à esquerda, links e botões à direita.
// - Celular: logo + botão ☰ que abre os mesmos links logo abaixo do cabeçalho.

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "@/components/Logo";
import { BotaoTema } from "@/components/Tema";
import { useAuth } from "@/contexts/AuthContext";

interface ItemMenu {
  href: string;
  texto: string;
  /** Decide se o link está ativo para a rota atual. */
  ativo: (caminho: string) => boolean;
}

const ITENS: ItemMenu[] = [
  { href: "/", texto: "Home", ativo: (c) => c === "/" },
  {
    href: "/eventos",
    texto: "Eventos",
    // /eventos e /eventos/123, mas não /eventos/novo (esse é o "Criar evento").
    ativo: (c) => c.startsWith("/eventos") && c !== "/eventos/novo",
  },
  { href: "/perfil", texto: "Perfil", ativo: (c) => c.startsWith("/perfil") },
];

export function BarraNavegacao() {
  const caminho = usePathname();
  const { usuario, logout } = useAuth();
  const [menuAberto, setMenuAberto] = useState(false);

  // Esc fecha o menu do celular.
  useEffect(() => {
    if (!menuAberto) return;
    const aoTeclar = (e: KeyboardEvent) => e.key === "Escape" && setMenuAberto(false);
    window.addEventListener("keydown", aoTeclar);
    return () => window.removeEventListener("keydown", aoTeclar);
  }, [menuAberto]);

  const fechar = () => setMenuAberto(false);
  const criarAtivo = caminho === "/eventos/novo";

  const estiloLink = (ativo: boolean) =>
    `rounded-lg px-3 py-2 font-medium transition ${
      ativo ? "bg-primary-light text-primary" : "text-texto-suave hover:bg-fundo hover:text-texto"
    }`;

  return (
    <header className="sticky top-0 z-30 border-b border-borda bg-superficie/95 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-4 px-4">
        <Link href="/" aria-label="sportfy. - ir para a Home" onClick={fechar}>
          {/* No modo escuro o cabeçalho fica azul-marinho: usa o logo verde-limão (a cor original). */}
          <Logo cor="escura" corEscuro="lima" tamanho="pequeno" prioridade />
        </Link>

        {/* Computador */}
        <nav aria-label="Navegação principal" className="hidden items-center gap-1 md:flex">
          {ITENS.map(({ href, texto, ativo }) => (
            <Link
              key={href}
              href={href}
              aria-current={ativo(caminho) ? "page" : undefined}
              className={estiloLink(ativo(caminho))}
            >
              {texto}
            </Link>
          ))}
          <Link
            href="/eventos/novo"
            aria-current={criarAtivo ? "page" : undefined}
            className={`ml-2 rounded-lg px-4 py-2 font-semibold text-white transition ${
              criarAtivo ? "bg-primary-dark" : "bg-primary hover:bg-primary-dark"
            }`}
          >
            + Criar evento
          </Link>
          <span className="mx-2 h-6 w-px bg-borda" aria-hidden />
          <BotaoTema className="text-texto-suave hover:bg-fundo hover:text-texto" />
          <span className="max-w-40 truncate text-sm text-texto-suave" title={usuario?.nome}>
            {usuario?.nome}
          </span>
          <button
            type="button"
            onClick={logout}
            className="rounded-lg px-3 py-2 text-sm font-medium text-texto-suave transition hover:bg-fundo hover:text-erro"
          >
            Sair
          </button>
        </nav>

        {/* Celular: tema + botão ☰ */}
        <div className="flex items-center gap-1 md:hidden">
          <BotaoTema className="text-texto hover:bg-fundo" />
          <button
            type="button"
            onClick={() => setMenuAberto((aberto) => !aberto)}
            aria-expanded={menuAberto}
            aria-controls="menu-celular"
            aria-label={menuAberto ? "Fechar menu" : "Abrir menu"}
            className="rounded-lg p-2 text-texto hover:bg-fundo"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className="h-6 w-6" aria-hidden>
              {menuAberto ? <path d="M6 6l12 12M18 6 6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
            </svg>
          </button>
        </div>
      </div>

      {/* Celular: menu aberto */}
      {menuAberto && (
        <nav
          id="menu-celular"
          aria-label="Navegação principal"
          className="border-t border-borda bg-superficie px-4 pb-4 pt-2 md:hidden"
        >
          <ul className="flex flex-col gap-1">
            {ITENS.map(({ href, texto, ativo }) => (
              <li key={href}>
                <Link
                  href={href}
                  onClick={fechar}
                  aria-current={ativo(caminho) ? "page" : undefined}
                  className={`block ${estiloLink(ativo(caminho))}`}
                >
                  {texto}
                </Link>
              </li>
            ))}
            <li>
              <Link
                href="/eventos/novo"
                onClick={fechar}
                aria-current={criarAtivo ? "page" : undefined}
                className="mt-1 block rounded-lg bg-primary px-3 py-2 text-center font-semibold text-white hover:bg-primary-dark"
              >
                + Criar evento
              </Link>
            </li>
            <li className="mt-2 flex items-center justify-between border-t border-borda pt-3">
              <span className="truncate text-sm text-texto-suave">{usuario?.nome}</span>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg px-3 py-2 text-sm font-medium text-erro hover:bg-fundo"
              >
                Sair
              </button>
            </li>
          </ul>
        </nav>
      )}
    </header>
  );
}
