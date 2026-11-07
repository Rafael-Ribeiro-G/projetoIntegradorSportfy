"use client";

// Editar evento: só quem criou, e só antes de começar (regra do PUT /api/partidas/{id}).

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormularioEvento, type DadosIniciaisEvento } from "@/components/FormularioEvento";
import { TituloPagina } from "@/components/TituloPagina";
import { useAuth } from "@/contexts/AuthContext";
import { tituloDaPartida } from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import { atualizarPartida, buscarPartida } from "@/services/partidas";
import type { Partida, PartidaRequest } from "@/types/partida";

export default function EditarEventoPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { usuario } = useAuth();

  const [partida, setPartida] = useState<Partida | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<{ status: number; mensagem: string } | null>(null);

  useEffect(() => {
    let ativo = true;
    buscarPartida(id)
      .then((p) => ativo && setPartida(p))
      .catch((e) => {
        if (!ativo) return;
        setErro({ status: e instanceof ApiError ? e.status : 0, mensagem: mensagemDeErro(e) });
      })
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [id]);

  async function salvar(dados: PartidaRequest) {
    await atualizarPartida(id, dados);
    router.push(`/eventos/${id}`);
  }

  if (carregando) {
    return (
      <main aria-busy className="mx-auto w-full max-w-3xl animate-pulse">
        <div className="mb-6 h-8 w-1/2 rounded bg-borda" />
        <div className="h-96 rounded-2xl bg-borda" />
      </main>
    );
  }

  // Motivo para não poder editar (ou null se pode).
  let bloqueio: string | null = null;
  if (erro || !partida) {
    bloqueio =
      erro?.status === 404 || erro?.status === 400
        ? "Evento não encontrado."
        : `Não foi possível abrir o evento: ${erro?.mensagem}`;
  } else if (partida.criador.id !== usuario?.id) {
    bloqueio = "Só quem criou o evento pode editá-lo.";
  } else if (partida.situacao !== "ABERTA" && partida.situacao !== "LOTADA") {
    bloqueio = "Este evento não pode mais ser editado (já começou, terminou ou foi cancelado).";
  }

  if (bloqueio || !partida) {
    return (
      <main className="mx-auto flex max-w-xl flex-col items-center gap-4 py-10 text-center">
        <h1 className="text-2xl font-bold">Não é possível editar</h1>
        <p className="text-texto-suave">{bloqueio}</p>
        <Link
          href={partida ? `/eventos/${id}` : "/eventos"}
          className="rounded-xl border border-primary px-6 py-3 font-semibold text-primary hover:bg-primary-light"
        >
          {partida ? "Voltar ao evento" : "Ver eventos"}
        </Link>
      </main>
    );
  }

  const inicial: DadosIniciaisEvento = {
    local: partida.local,
    quadraId: partida.quadra?.id,
    esporteId: partida.esporte.id,
    titulo: partida.titulo,
    descricao: partida.descricao,
    imagemUrl: partida.imagemUrl,
    dataHoraInicio: partida.dataHoraInicio,
    dataHoraFim: partida.dataHoraFim,
    maxJogadores: partida.maxJogadores,
    participantesConfirmados: partida.participantesConfirmados,
  };

  return (
    <main className="mx-auto w-full max-w-3xl">
      <Link href={`/eventos/${id}`} className="mb-4 inline-block text-sm font-semibold text-primary hover:underline">
        ← Voltar ao evento
      </Link>
      <TituloPagina titulo="Editar evento" subtitulo={tituloDaPartida(partida)} />
      <FormularioEvento inicial={inicial} textoBotao="Salvar alterações" aoEnviar={salvar} />
    </main>
  );
}
