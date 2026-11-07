"use client";

// Escolha de esportes favoritos em "chips" (checkboxes estilizados). Busca a lista sozinho.

import { useEffect, useState } from "react";
import { Alerta } from "@/components/Alerta";
import { mensagemDeErro } from "@/services/api";
import { listarEsportes } from "@/services/esportes";
import type { Esporte } from "@/types/esporte";

export function SeletorEsportes({
  selecionados,
  aoMudar,
}: {
  selecionados: number[];
  aoMudar: (ids: number[]) => void;
}) {
  const [esportes, setEsportes] = useState<Esporte[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let ativo = true;
    listarEsportes()
      .then((lista) => ativo && setEsportes(lista))
      .catch((e) => ativo && setErro(mensagemDeErro(e)))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, []);

  function alternar(id: number) {
    aoMudar(selecionados.includes(id) ? selecionados.filter((x) => x !== id) : [...selecionados, id]);
  }

  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium">Esportes favoritos</legend>
      {carregando && <p className="text-sm text-texto-suave">Carregando esportes...</p>}
      {erro && <Alerta tipo="aviso">Não foi possível carregar os esportes: {erro}</Alerta>}
      {!carregando && !erro && esportes.length === 0 && (
        <p className="text-sm text-texto-suave">Nenhum esporte cadastrado.</p>
      )}
      <div className="flex flex-wrap gap-2">
        {esportes.map((esporte) => {
          const marcado = selecionados.includes(esporte.id);
          return (
            <label
              key={esporte.id}
              className={`cursor-pointer rounded-full border px-4 py-2 text-sm transition has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary ${
                marcado ? "border-primary bg-primary text-white" : "border-borda bg-superficie hover:border-primary"
              }`}
            >
              <input
                type="checkbox"
                className="sr-only"
                checked={marcado}
                onChange={() => alternar(esporte.id)}
              />
              {esporte.nome}
            </label>
          );
        })}
      </div>
    </fieldset>
  );
}
