/** Título de cada tela logada, com subtítulo e ações opcionais à direita (ex.: botão "Editar"). */
export function TituloPagina({
  titulo,
  subtitulo,
  children,
}: {
  titulo: string;
  subtitulo?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-bold md:text-3xl">{titulo}</h1>
        {subtitulo && <p className="mt-1 text-texto-suave">{subtitulo}</p>}
      </div>
      {children && <div className="flex gap-2">{children}</div>}
    </div>
  );
}
