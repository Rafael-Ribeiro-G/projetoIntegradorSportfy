"use client";

// Home: saudação + "meus próximos eventos" + sugestões (perto de mim, se a pessoa deixar).

import { useEffect, useState } from "react";
import Link from "next/link";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { EventoCard, EventoCardEsqueleto } from "@/components/EventoCard";
import { EventoItemLista } from "@/components/EventoItemLista";
import { useAuth } from "@/contexts/AuthContext";
import { apenasProximos, juntarMeusEventos, type MeuEvento } from "@/lib/eventos";
import { obterPosicao, type Posicao } from "@/lib/geolocalizacao";
import { mensagemDeErro } from "@/services/api";
import { listarSugestoes } from "@/services/partidas";
import { buscarMinhasPartidas } from "@/services/usuarios";
import type { Partida } from "@/types/partida";

const LIMITE_SUGESTOES = 5;
const MAX_MEUS_EVENTOS = 5;

export default function Home() {
  const { usuario } = useAuth();
  const primeiroNome = usuario?.nome.split(" ")[0] ?? "";

  // ---------- meus próximos eventos ----------
  const [meus, setMeus] = useState<MeuEvento[]>([]);
  const [carregandoMeus, setCarregandoMeus] = useState(true);
  const [erroMeus, setErroMeus] = useState("");
  const [versaoMeus, setVersaoMeus] = useState(0);

  useEffect(() => {
    let ativo = true;
    buscarMinhasPartidas()
      .then((dados) => {
        if (!ativo) return;
        setMeus(apenasProximos(juntarMeusEventos(dados)));
        setErroMeus("");
      })
      .catch((e) => ativo && setErroMeus(mensagemDeErro(e)))
      .finally(() => ativo && setCarregandoMeus(false));
    return () => {
      ativo = false;
    };
  }, [versaoMeus]);

  // ---------- localização ----------
  // undefined = ainda perguntando ao navegador; null = negada/indisponível.
  const [posicao, setPosicao] = useState<Posicao | null | undefined>(undefined);
  const [tentativaLocal, setTentativaLocal] = useState(0);

  useEffect(() => {
    let ativo = true;
    obterPosicao(8_000).then((p) => ativo && setPosicao(p));
    return () => {
      ativo = false;
    };
  }, [tentativaLocal]);

  // ---------- sugestões (esperam a resposta da localização) ----------
  const [sugestoes, setSugestoes] = useState<Partida[]>([]);
  const [carregandoSugestoes, setCarregandoSugestoes] = useState(true);
  const [erroSugestoes, setErroSugestoes] = useState("");
  const [versaoSugestoes, setVersaoSugestoes] = useState(0);

  useEffect(() => {
    if (posicao === undefined) return; // ainda esperando o navegador
    let ativo = true;
    listarSugestoes({
      latitude: posicao?.latitude,
      longitude: posicao?.longitude,
      limite: LIMITE_SUGESTOES,
    })
      .then((lista) => {
        if (!ativo) return;
        setSugestoes(lista);
        setErroSugestoes("");
      })
      .catch((e) => ativo && setErroSugestoes(mensagemDeErro(e)))
      .finally(() => ativo && setCarregandoSugestoes(false));
    return () => {
      ativo = false;
    };
  }, [posicao, versaoSugestoes]);

  function tentarMeusDeNovo() {
    setCarregandoMeus(true);
    setVersaoMeus((v) => v + 1);
  }

  function tentarSugestoesDeNovo() {
    setCarregandoSugestoes(true);
    setVersaoSugestoes((v) => v + 1);
  }

  function usarMinhaLocalizacao() {
    setCarregandoSugestoes(true);
    setPosicao(undefined);
    setTentativaLocal((t) => t + 1);
  }

  return (
    <main className="flex flex-col gap-10">
      {/* Saudação */}
      <section className="flex flex-col gap-4 rounded-2xl bg-gradient-to-br from-primary to-primary-dark p-6 text-white shadow-sm sm:flex-row sm:items-center sm:justify-between sm:p-8">
        <div>
          <h1 className="text-2xl font-bold sm:text-3xl">Olá, {primeiroNome}! 👋</h1>
          <p className="mt-1 text-white/85">Bora praticar hoje? Pratique, Conecte e Divirta-se.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            href="/eventos"
            className="rounded-xl bg-white px-5 py-3 font-semibold text-primary-dark transition hover:bg-white/90"
          >
            Encontrar eventos
          </Link>
          <Link
            href="/eventos/novo"
            className="rounded-xl border border-white/70 px-5 py-3 font-semibold text-white transition hover:bg-white/10"
          >
            + Criar evento
          </Link>
        </div>
      </section>

      {/* Meus próximos eventos */}
      <section aria-labelledby="titulo-meus" aria-busy={carregandoMeus}>
        <div className="mb-4 flex items-end justify-between gap-4">
          <h2 id="titulo-meus" className="text-xl font-bold">
            Meus próximos eventos
          </h2>
          {meus.length > MAX_MEUS_EVENTOS && (
            <Link href="/perfil" className="text-sm font-semibold text-primary hover:underline">
              Ver todos ({meus.length})
            </Link>
          )}
        </div>

        {carregandoMeus ? (
          <div className="grid gap-3 md:grid-cols-2">
            {Array.from({ length: 2 }, (_, i) => (
              <div key={i} aria-hidden className="h-20 animate-pulse rounded-2xl bg-borda" />
            ))}
          </div>
        ) : erroMeus ? (
          <div className="flex flex-col items-start gap-3">
            <Alerta>Não foi possível carregar seus eventos: {erroMeus}</Alerta>
            <Botao variante="secundario" className="w-auto! px-6" onClick={tentarMeusDeNovo}>
              Tentar de novo
            </Botao>
          </div>
        ) : meus.length === 0 ? (
          <div className="rounded-2xl bg-superficie p-6 text-center shadow-sm ring-1 ring-borda">
            <p className="font-semibold">Você ainda não tem eventos marcados</p>
            <p className="mt-1 text-sm text-texto-suave">
              Entre em uma das sugestões abaixo ou{" "}
              <Link href="/eventos/novo" className="font-semibold text-primary hover:underline">
                crie o seu
              </Link>
              .
            </p>
          </div>
        ) : (
          <ul className="grid gap-3 md:grid-cols-2">
            {meus.slice(0, MAX_MEUS_EVENTOS).map(({ partida, organizador }) => (
              <li key={partida.id}>
                <EventoItemLista partida={partida} organizador={organizador} />
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Sugestões */}
      <section aria-labelledby="titulo-sugestoes" aria-busy={carregandoSugestoes}>
        <div className="mb-4 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 id="titulo-sugestoes" className="text-xl font-bold">
              Sugestões para você
            </h2>
            <p className="text-sm text-texto-suave">
              {posicao
                ? "Eventos com vaga perto de você, priorizando seus esportes favoritos."
                : "Eventos com vaga, priorizando seus esportes favoritos."}
            </p>
          </div>
          <Link href="/eventos" className="text-sm font-semibold text-primary hover:underline">
            Ver todos os eventos
          </Link>
        </div>

        {posicao === null && (
          <div className="mb-4 flex flex-wrap items-center gap-3 rounded-xl bg-fundo px-4 py-3 text-sm ring-1 ring-borda">
            <span className="text-texto-suave">
              📍 Ative a localização para ver os eventos mais perto de você.
            </span>
            <button
              type="button"
              onClick={usarMinhaLocalizacao}
              className="font-semibold text-primary hover:underline"
            >
              Usar minha localização
            </button>
          </div>
        )}

        {carregandoSugestoes ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }, (_, i) => (
              <EventoCardEsqueleto key={i} />
            ))}
          </div>
        ) : erroSugestoes ? (
          <div className="flex flex-col items-start gap-3">
            <Alerta>Não foi possível carregar as sugestões: {erroSugestoes}</Alerta>
            <Botao variante="secundario" className="w-auto! px-6" onClick={tentarSugestoesDeNovo}>
              Tentar de novo
            </Botao>
          </div>
        ) : sugestoes.length === 0 ? (
          <div className="rounded-2xl bg-superficie p-6 text-center shadow-sm ring-1 ring-borda">
            <p className="font-semibold">Nenhuma sugestão no momento</p>
            <p className="mt-1 text-sm text-texto-suave">
              Não há eventos com vaga que combinem com você agora.{" "}
              <Link href="/eventos" className="font-semibold text-primary hover:underline">
                Veja todos os eventos
              </Link>
              .
            </p>
          </div>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {sugestoes.map((p) => (
              <EventoCard key={p.id} partida={p} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
