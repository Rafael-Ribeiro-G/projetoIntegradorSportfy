"use client";

// Controles do modo escuro.
// - BotaoTema: alterna claro ↔ escuro com um clique (cabeçalho e telas de entrada).
// - SeletorTema: as três opções, incluindo "Sistema" (seção Aparência do Perfil).

import { useTema, type PreferenciaTema } from "@/lib/tema";

const IconeSol = ({ className = "h-5 w-5" }: { className?: string }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className={className} aria-hidden>
    <circle cx="12" cy="12" r="4" />
    <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
  </svg>
);

const IconeLua = ({ className = "h-5 w-5" }: { className?: string }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden>
    <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
  </svg>
);

const IconeTela = ({ className = "h-5 w-5" }: { className?: string }) => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className={className} aria-hidden>
    <rect x="3" y="4" width="18" height="12" rx="2" />
    <path d="M8 20h8M12 16v4" />
  </svg>
);

/** Botão de um clique: mostra a lua no claro (para ir ao escuro) e o sol no escuro. */
export function BotaoTema({ className = "" }: { className?: string }) {
  const { tema, definirTema } = useTema();
  const escuro = tema === "escuro";

  return (
    <button
      type="button"
      onClick={() => definirTema(escuro ? "claro" : "escuro")}
      aria-label={escuro ? "Ativar modo claro" : "Ativar modo escuro"}
      title={escuro ? "Modo claro" : "Modo escuro"}
      className={`rounded-lg p-2 transition ${className}`}
    >
      {escuro ? <IconeSol /> : <IconeLua />}
    </button>
  );
}

const OPCOES: { valor: PreferenciaTema; texto: string; Icone: typeof IconeSol }[] = [
  { valor: "claro", texto: "Claro", Icone: IconeSol },
  { valor: "escuro", texto: "Escuro", Icone: IconeLua },
  { valor: "sistema", texto: "Sistema", Icone: IconeTela },
];

/** As três opções lado a lado (grupo de rádios estilizado). */
export function SeletorTema() {
  const { preferencia, definirTema } = useTema();

  return (
    <fieldset>
      <legend className="sr-only">Tema do site</legend>
      <div className="grid grid-cols-3 gap-2">
        {OPCOES.map(({ valor, texto, Icone }) => {
          const marcada = preferencia === valor;
          return (
            <label
              key={valor}
              className={`flex cursor-pointer flex-col items-center gap-2 rounded-xl border p-3 text-sm font-medium transition has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary ${
                marcada
                  ? "border-primary bg-primary-light text-primary-forte"
                  : "border-borda text-texto-suave hover:border-primary"
              }`}
            >
              <input
                type="radio"
                name="tema"
                value={valor}
                checked={marcada}
                onChange={() => definirTema(valor)}
                className="sr-only"
              />
              <Icone className="h-6 w-6" />
              {texto}
            </label>
          );
        })}
      </div>
      <p className="mt-2 text-sm text-texto-suave">
        &quot;Sistema&quot; segue a configuração do seu computador ou celular.
      </p>
    </fieldset>
  );
}
