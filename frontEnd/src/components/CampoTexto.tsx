import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

// Input com label e mensagem de erro embaixo (para os erros 400 da API).

const estiloCampo = (erro?: string) =>
  `w-full rounded-xl border bg-superficie px-4 py-3 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary-light ${
    erro ? "border-erro" : "border-borda"
  }`;

interface BaseProps {
  id: string;
  label: string;
  erro?: string;
  /** Texto de ajuda, embaixo do campo (some quando há erro). */
  dica?: string;
}

function Rodape({ id, erro, dica }: Omit<BaseProps, "label">) {
  if (erro) {
    return (
      <p id={`${id}-erro`} className="text-sm text-erro">
        {erro}
      </p>
    );
  }
  if (dica) {
    return (
      <p id={`${id}-dica`} className="text-sm text-texto-suave">
        {dica}
      </p>
    );
  }
  return null;
}

function descricaoDe({ id, erro, dica }: Omit<BaseProps, "label">) {
  if (erro) return `${id}-erro`;
  if (dica) return `${id}-dica`;
  return undefined;
}

export function CampoTexto({
  id,
  label,
  erro,
  dica,
  ...resto
}: BaseProps & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <input
        id={id}
        name={id}
        aria-invalid={!!erro}
        aria-describedby={descricaoDe({ id, erro, dica })}
        className={estiloCampo(erro)}
        {...resto}
      />
      <Rodape id={id} erro={erro} dica={dica} />
    </div>
  );
}

export function CampoSelecao({
  id,
  label,
  erro,
  dica,
  children,
  ...resto
}: BaseProps & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <select
        id={id}
        name={id}
        aria-invalid={!!erro}
        aria-describedby={descricaoDe({ id, erro, dica })}
        className={estiloCampo(erro)}
        {...resto}
      >
        {children}
      </select>
      <Rodape id={id} erro={erro} dica={dica} />
    </div>
  );
}

export function CampoAreaTexto({
  id,
  label,
  erro,
  dica,
  ...resto
}: BaseProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <textarea
        id={id}
        name={id}
        rows={3}
        aria-invalid={!!erro}
        aria-describedby={descricaoDe({ id, erro, dica })}
        className={`${estiloCampo(erro)} resize-none`}
        {...resto}
      />
      <Rodape id={id} erro={erro} dica={dica} />
    </div>
  );
}
