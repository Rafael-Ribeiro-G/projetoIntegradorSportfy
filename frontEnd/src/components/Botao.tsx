import type { ButtonHTMLAttributes } from "react";

type Variante = "primario" | "secundario" | "perigo" | "texto";

interface BotaoProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante;
  /** Mostra "Aguarde..." e desabilita o botão (evita clique duplo). */
  carregando?: boolean;
}

const estilos: Record<Variante, string> = {
  primario: "bg-primary text-white hover:bg-primary-dark",
  secundario: "border border-primary text-primary hover:bg-primary-light",
  perigo: "bg-erro text-white hover:opacity-90",
  texto: "text-primary hover:underline",
};

export function Botao({
  variante = "primario",
  carregando = false,
  disabled,
  className = "",
  children,
  type = "button",
  ...resto
}: BotaoProps) {
  return (
    <button
      type={type}
      disabled={disabled || carregando}
      aria-busy={carregando}
      className={`w-full rounded-xl px-4 py-3 font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${estilos[variante]} ${className}`}
      {...resto}
    >
      {carregando ? "Aguarde..." : children}
    </button>
  );
}
