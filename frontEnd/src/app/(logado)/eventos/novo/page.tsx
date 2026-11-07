"use client";

import { useRouter } from "next/navigation";
import { FormularioEvento } from "@/components/FormularioEvento";
import { TituloPagina } from "@/components/TituloPagina";
import { criarPartida } from "@/services/partidas";
import type { PartidaRequest } from "@/types/partida";

export default function NovoEventoPage() {
  const router = useRouter();

  async function criar(dados: PartidaRequest) {
    const partida = await criarPartida(dados);
    // Quem cria já entra como participante; abre o detalhe do evento novo.
    router.push(`/eventos/${partida.id}`);
  }

  return (
    <main className="mx-auto w-full max-w-3xl">
      <TituloPagina titulo="Criar evento" subtitulo="Chame a galera para jogar, correr ou treinar" />
      <FormularioEvento textoBotao="Criar evento" aoEnviar={criar} />
    </main>
  );
}
