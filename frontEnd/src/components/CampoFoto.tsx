"use client";

// Campo de foto de perfil: prévia redonda + escolher/trocar/remover.
// Valida tipo e tamanho aqui; o upload de verdade acontece quando o formulário é salvo.

import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { Avatar } from "@/components/Avatar";
import { TIPOS_IMAGEM_ACEITOS, validarImagem } from "@/services/uploads";

export interface ValorFoto {
  /** Arquivo novo escolhido (ainda não enviado). */
  arquivo: File | null;
  /** true se a pessoa removeu a foto que já existia. */
  removida: boolean;
}

interface Props {
  id: string;
  nome: string; // para as iniciais quando não há foto
  /** Foto que já está no servidor (tela Editar). */
  fotoAtual?: string;
  aoMudar: (valor: ValorFoto) => void;
  /** Erro vindo de fora (ex.: o upload falhou). */
  erro?: string;
}

export function CampoFoto({ id, nome, fotoAtual, aoMudar, erro }: Props) {
  const [preview, setPreview] = useState<string | null>(null);
  const [removida, setRemovida] = useState(false);
  const [erroLocal, setErroLocal] = useState("");
  const previewRef = useRef<string | null>(null);

  // Libera a URL temporária da prévia ao sair da tela.
  useEffect(
    () => () => {
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    },
    [],
  );

  function trocarPreview(nova: string | null) {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = nova;
    setPreview(nova);
  }

  function aoEscolher(e: ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    e.target.value = ""; // permite escolher o mesmo arquivo de novo
    if (!arquivo) return;

    const problema = validarImagem(arquivo);
    if (problema) {
      setErroLocal(problema);
      return;
    }
    setErroLocal("");
    setRemovida(false);
    trocarPreview(URL.createObjectURL(arquivo));
    aoMudar({ arquivo, removida: false });
  }

  function remover() {
    trocarPreview(null);
    setRemovida(true);
    setErroLocal("");
    aoMudar({ arquivo: null, removida: true });
  }

  const mostrada = preview ?? (removida ? undefined : fotoAtual);
  const mensagemErro = erroLocal || erro;

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium">Foto de perfil</span>
      <div className="flex items-center gap-4">
        <Avatar nome={nome || "?"} fotoUrl={mostrada} tamanho="medio" />
        <div className="flex flex-wrap items-center gap-3">
          <label
            htmlFor={id}
            className="cursor-pointer rounded-lg bg-primary-light px-3 py-2 text-sm font-semibold text-primary has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary"
          >
            {mostrada ? "Trocar foto" : "Escolher foto"}
            <input
              id={id}
              type="file"
              accept={TIPOS_IMAGEM_ACEITOS.join(",")}
              onChange={aoEscolher}
              aria-describedby={`${id}-dica`}
              className="sr-only"
            />
          </label>
          {mostrada && (
            <button type="button" onClick={remover} className="text-sm text-texto-suave underline">
              Remover foto
            </button>
          )}
        </div>
      </div>
      {mensagemErro ? (
        <p id={`${id}-dica`} className="text-sm text-erro">
          {mensagemErro}
        </p>
      ) : (
        <p id={`${id}-dica`} className="text-sm text-texto-suave">
          JPG, PNG ou WEBP de até 5 MB.
        </p>
      )}
    </div>
  );
}
