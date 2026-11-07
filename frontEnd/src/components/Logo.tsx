/** Logo em texto: "sportfy." (minúsculo, com ponto) + slogan opcional. */
export function Logo({
  comSlogan = false,
  tamanho = "grande",
  claro = false,
}: {
  comSlogan?: boolean;
  /** "pequeno" para o cabeçalho, "grande" para telas de entrada. */
  tamanho?: "pequeno" | "grande";
  /** Texto branco, para usar sobre fundo verde. */
  claro?: boolean;
}) {
  const tamanhoTexto = tamanho === "pequeno" ? "text-2xl" : "text-5xl";
  return (
    <span className="inline-flex flex-col gap-1">
      <span className={`${tamanhoTexto} font-extrabold tracking-tight ${claro ? "text-white" : "text-primary"}`}>
        sportfy<span className="text-secondary">.</span>
      </span>
      {comSlogan && (
        <span className={claro ? "text-white/85" : "text-texto-suave"}>
          Pratique, Conecte e Divirta-se
        </span>
      )}
    </span>
  );
}
