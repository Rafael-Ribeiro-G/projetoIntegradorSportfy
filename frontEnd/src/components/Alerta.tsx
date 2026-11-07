type Tipo = "erro" | "aviso" | "sucesso" | "info";

const estilos: Record<Tipo, string> = {
  erro: "border-erro/30 bg-erro/10 text-erro",
  aviso: "border-aviso/30 bg-aviso/10 text-aviso",
  sucesso: "border-sucesso/30 bg-sucesso/10 text-sucesso",
  info: "border-borda bg-fundo text-texto-suave",
};

/** Caixa de mensagem. role="alert" faz o leitor de tela anunciar o erro. */
export function Alerta({ tipo = "erro", children }: { tipo?: Tipo; children: React.ReactNode }) {
  return (
    <div
      role={tipo === "erro" ? "alert" : "status"}
      className={`rounded-xl border px-4 py-3 text-sm ${estilos[tipo]}`}
    >
      {children}
    </div>
  );
}
