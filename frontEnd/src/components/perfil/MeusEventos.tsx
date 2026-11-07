"use client";

// Lista completa dos meus eventos (criados + participando), em abas Próximos / Anteriores.

import { useEffect, useState } from "react";
import Link from "next/link";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { EventoItemLista } from "@/components/EventoItemLista";
import { apenasProximos, juntarMeusEventos, type MeuEvento } from "@/lib/eventos";
import { mensagemDeErro } from "@/services/api";
import { buscarMinhasPartidas } from "@/services/usuarios";

type Aba = "proximos" | "anteriores";

export function MeusEventos() {
  const [todos, setTodos] = useState<MeuEvento[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [versao, setVersao] = useState(0);
  const [aba, setAba] = useState<Aba>("proximos");

  useEffect(() => {
    let ativo = true;
    buscarMinhasPartidas()
      .then((dados) => {
        if (!ativo) return;
        setTodos(juntarMeusEventos(dados));
        setErro("");
      })
      .catch((e) => ativo && setErro(mensagemDeErro(e)))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [versao]);

  const proximos = apenasProximos(todos);
  // Anteriores: do mais recente para o mais antigo.
  const anteriores = todos.filter((e) => !proximos.includes(e)).reverse();
  const lista = aba === "proximos" ? proximos : anteriores;

  const estiloAba = (ativa: boolean) =>
    `rounded-lg px-4 py-2 text-sm font-semibold transition ${
      ativa ? "bg-primary text-white" : "text-texto-suave hover:bg-fundo"
    }`;

  return (
    <section aria-labelledby="titulo-meus-eventos" className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 id="titulo-meus-eventos" className="text-lg font-semibold">
          Meus eventos
        </h2>
        <div role="tablist" aria-label="Filtrar meus eventos" className="flex gap-1 rounded-xl bg-fundo p-1">
          <button
            type="button"
            role="tab"
            aria-selected={aba === "proximos"}
            onClick={() => setAba("proximos")}
            className={estiloAba(aba === "proximos")}
          >
            Próximos ({proximos.length})
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={aba === "anteriores"}
            onClick={() => setAba("anteriores")}
            className={estiloAba(aba === "anteriores")}
          >
            Anteriores ({anteriores.length})
          </button>
        </div>
      </div>

      <div role="tabpanel" aria-busy={carregando}>
        {carregando ? (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 2 }, (_, i) => (
              <div key={i} aria-hidden className="h-20 animate-pulse rounded-2xl bg-borda" />
            ))}
          </div>
        ) : erro ? (
          <div className="flex flex-col items-start gap-3">
            <Alerta>Não foi possível carregar seus eventos: {erro}</Alerta>
            <Botao
              variante="secundario"
              className="w-auto! px-6"
              onClick={() => {
                setCarregando(true);
                setVersao((v) => v + 1);
              }}
            >
              Tentar de novo
            </Botao>
          </div>
        ) : lista.length === 0 ? (
          <p className="py-4 text-center text-sm text-texto-suave">
            {aba === "proximos" ? (
              <>
                Nenhum evento marcado.{" "}
                <Link href="/eventos" className="font-semibold text-primary hover:underline">
                  Encontre um evento
                </Link>{" "}
                ou{" "}
                <Link href="/eventos/novo" className="font-semibold text-primary hover:underline">
                  crie o seu
                </Link>
                .
              </>
            ) : (
              "Você ainda não tem eventos anteriores."
            )}
          </p>
        ) : (
          <ul className="flex flex-col gap-3">
            {lista.map(({ partida, organizador }) => (
              <li key={partida.id}>
                <EventoItemLista partida={partida} organizador={organizador} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
