"use client";

// Detalhe do evento: informações, vagas, botão Participar/Sair/Cancelar e participantes.

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Alerta } from "@/components/Alerta";
import { Avatar } from "@/components/Avatar";
import { Botao } from "@/components/Botao";
import { ImagemCapa } from "@/components/ImagemCapa";
import { SituacaoBadge } from "@/components/SituacaoBadge";
import { useAuth } from "@/contexts/AuthContext";
import { formatarDiaLongo, formatarDuracao, formatarHora } from "@/lib/datas";
import {
  formatarDistancia,
  formatarPreco,
  textoNivel,
  textoVagas,
  TIPOS_LOCAL,
  tituloDaPartida,
} from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import {
  buscarPartida,
  cancelarPartida,
  listarParticipantes,
  participarDaPartida,
  sairDaPartida,
} from "@/services/partidas";
import type { Participante, Partida, SituacaoPartida } from "@/types/partida";

type Acao = "participar" | "sair" | "cancelar";

/** Por que não dá para participar, conforme a situação. */
const MOTIVO_SEM_PARTICIPAR: Partial<Record<SituacaoPartida, string>> = {
  LOTADA: "Evento lotado",
  EM_ANDAMENTO: "O evento já começou",
  ENCERRADA: "Evento encerrado",
  CANCELADA: "Evento cancelado",
};

/** Link do Google Maps pelo ponto (se tiver) ou pelo endereço. */
function linkDoMapa(p: Partida): string | undefined {
  const { latitude, longitude, endereco, nome } = p.local;
  if (latitude !== undefined && longitude !== undefined) {
    return `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;
  }
  if (endereco) {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${nome}, ${endereco}`)}`;
  }
  return undefined;
}

export default function DetalheEventoPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { usuario } = useAuth();

  const [partida, setPartida] = useState<Partida | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<{ status: number; mensagem: string } | null>(null);

  const [participantes, setParticipantes] = useState<Participante[]>([]);
  const [erroParticipantes, setErroParticipantes] = useState("");

  const [acao, setAcao] = useState<Acao | null>(null);
  const [aviso, setAviso] = useState<{ tipo: "sucesso" | "erro"; texto: string } | null>(null);
  const [linkCopiado, setLinkCopiado] = useState(false);

  // Muda "versao" para buscar tudo de novo (ex.: depois de participar).
  const [versao, setVersao] = useState(0);
  const recarregar = () => setVersao((v) => v + 1);

  /** Busca o evento e os participantes ao mesmo tempo; aplica no .then(). */
  useEffect(() => {
    let ativo = true; // ignora a resposta se a pessoa já saiu da tela
    Promise.allSettled([buscarPartida(id), listarParticipantes(id)]).then(
      ([resPartida, resParticipantes]) => {
        if (!ativo) return;

        if (resPartida.status === "fulfilled") {
          setPartida(resPartida.value);
          setErro(null);
        } else {
          const e = resPartida.reason;
          setErro({ status: e instanceof ApiError ? e.status : 0, mensagem: mensagemDeErro(e) });
        }

        if (resParticipantes.status === "fulfilled") {
          setParticipantes(resParticipantes.value);
          setErroParticipantes("");
        } else {
          setErroParticipantes(mensagemDeErro(resParticipantes.reason));
        }

        setCarregando(false);
      },
    );
    return () => {
      ativo = false;
    };
  }, [id, versao]);

  /** Executa uma ação (participar/sair/cancelar), mostra o resultado e recarrega a tela. */
  async function executar(tipo: Acao, chamada: () => Promise<unknown>, sucesso: string) {
    setAcao(tipo);
    setAviso(null);
    try {
      await chamada();
      setAviso({ tipo: "sucesso", texto: sucesso });
      recarregar();
    } catch (e) {
      // 409 (já participa), 422 (lotada/cancelada/começou), 403... a API manda a mensagem pronta.
      setAviso({ tipo: "erro", texto: mensagemDeErro(e) });
    } finally {
      setAcao(null);
    }
  }

  function participar() {
    executar("participar", () => participarDaPartida(id), "Pronto! Você está participando. 🎉");
  }

  function sair() {
    if (!window.confirm("Quer mesmo sair deste evento?")) return;
    executar("sair", () => sairDaPartida(id), "Você saiu do evento.");
  }

  function cancelar() {
    const ok = window.confirm(
      "Cancelar este evento? Ninguém mais poderá entrar e os participantes verão que ele foi cancelado. Isso não pode ser desfeito.",
    );
    if (!ok) return;
    executar("cancelar", () => cancelarPartida(id), "Evento cancelado.");
  }

  async function copiarLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setLinkCopiado(true);
      setTimeout(() => setLinkCopiado(false), 2500);
    } catch {
      window.prompt("Copie o link do evento:", window.location.href);
    }
  }

  function voltar() {
    // Volta para a lista com os filtros que a pessoa tinha; se abriu direto pelo link, vai para /eventos.
    if (window.history.length > 1) router.back();
    else router.push("/eventos");
  }

  // ---------- estados de carregando / erro ----------

  if (carregando) {
    return (
      <main aria-busy className="animate-pulse">
        <div className="mb-6 h-8 w-1/2 rounded bg-borda" />
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="aspect-video rounded-2xl bg-borda lg:col-span-2" />
          <div className="h-64 rounded-2xl bg-borda" />
        </div>
      </main>
    );
  }

  if (erro || !partida) {
    const naoExiste = erro?.status === 404 || erro?.status === 400;
    return (
      <main className="mx-auto flex max-w-xl flex-col items-center gap-4 py-10 text-center">
        <h1 className="text-2xl font-bold">
          {naoExiste ? "Evento não encontrado" : "Não foi possível abrir o evento"}
        </h1>
        <p className="text-texto-suave">
          {naoExiste ? "Ele pode ter sido removido ou o link está errado." : erro?.mensagem}
        </p>
        <div className="flex gap-2">
          {!naoExiste && (
            <Botao className="w-auto! px-6" onClick={() => { setCarregando(true); recarregar(); }}>
              Tentar de novo
            </Botao>
          )}
          <Link href="/eventos" className="rounded-xl border border-primary px-6 py-3 font-semibold text-primary hover:bg-primary-light">
            Ver eventos
          </Link>
        </div>
      </main>
    );
  }

  // ---------- tela do evento ----------

  const titulo = tituloDaPartida(partida);
  const ehCriador = usuario?.id === partida.criador.id;
  const participa =
    partida.usuarioParticipa ?? participantes.some((p) => p.id === usuario?.id);
  const encerradoOuCancelado = partida.situacao === "ENCERRADA" || partida.situacao === "CANCELADA";
  const mapa = linkDoMapa(partida);
  const confirmados = partida.participantesConfirmados ?? participantes.length;
  const porcentagem =
    partida.maxJogadores ? Math.min(100, Math.round((confirmados / partida.maxJogadores) * 100)) : 0;

  return (
    <main>
      <button
        type="button"
        onClick={voltar}
        className="mb-4 text-sm font-semibold text-primary hover:underline"
      >
        ← Voltar
      </button>

      {/*
        Ordem no celular: capa/título → vagas e botão → detalhes (a ação aparece logo).
        No computador (lg): capa/título e detalhes à esquerda; vagas/botão/participantes fixos à direita.
      */}
      <div className="grid gap-6 lg:grid-cols-3 lg:grid-rows-[auto_1fr]">
        <header className="flex flex-col gap-6 lg:col-span-2">
          <div className="relative aspect-video overflow-hidden rounded-2xl shadow-sm lg:aspect-21/9">
            <ImagemCapa src={partida.imagemUrl} alt={`Capa: ${titulo}`} esporte={partida.esporte.nome} />
          </div>

          <div className="flex flex-col gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-semibold uppercase tracking-wide text-primary">
                {partida.esporte.nome}
              </span>
              <SituacaoBadge situacao={partida.situacao} />
            </div>
            <h1 className="text-3xl font-bold leading-tight">{titulo}</h1>
            <p className="text-texto-suave">
              Organizado por{" "}
              <Link
                href={ehCriador ? "/perfil" : `/usuarios/${partida.criador.id}`}
                className="font-semibold text-texto hover:text-primary hover:underline"
              >
                {ehCriador ? "você" : partida.criador.nome}
              </Link>
            </p>
          </div>
        </header>

        <aside className="flex flex-col gap-6 lg:sticky lg:top-24 lg:col-start-3 lg:row-span-2 lg:row-start-1 lg:self-start">
          <section className="flex flex-col gap-4 rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda">
            <div>
              <p className="text-2xl font-bold">{textoVagas(partida)}</p>
              <p className="text-sm text-texto-suave">
                {confirmados} confirmado{confirmados === 1 ? "" : "s"}
                {partida.maxJogadores !== undefined && ` de ${partida.maxJogadores}`}
              </p>
              {partida.maxJogadores !== undefined && (
                <div
                  role="progressbar"
                  aria-label="Vagas preenchidas"
                  aria-valuenow={confirmados}
                  aria-valuemin={0}
                  aria-valuemax={partida.maxJogadores}
                  className="mt-3 h-2 overflow-hidden rounded-full bg-fundo"
                >
                  <div className="h-full rounded-full bg-primary" style={{ width: `${porcentagem}%` }} />
                </div>
              )}
            </div>

            {aviso && <Alerta tipo={aviso.tipo}>{aviso.texto}</Alerta>}

            {/* Botões conforme quem está vendo */}
            {ehCriador ? (
              <>
                <p className="text-sm text-texto-suave">Você é o organizador deste evento.</p>
                {/* Editar: só antes de começar (o PUT da API recusa depois). */}
                {(partida.situacao === "ABERTA" || partida.situacao === "LOTADA") && (
                  <Link
                    href={`/eventos/${partida.id}/editar`}
                    className="rounded-xl border border-primary px-4 py-3 text-center font-semibold text-primary transition hover:bg-primary-light"
                  >
                    Editar evento
                  </Link>
                )}
                {!encerradoOuCancelado && (
                  <Botao variante="perigo" carregando={acao === "cancelar"} disabled={!!acao} onClick={cancelar}>
                    Cancelar evento
                  </Botao>
                )}
              </>
            ) : participa ? (
              <>
                <p className="text-sm font-semibold text-primary">✓ Você está participando</p>
                {!encerradoOuCancelado && partida.situacao !== "EM_ANDAMENTO" && (
                  <Botao variante="secundario" carregando={acao === "sair"} disabled={!!acao} onClick={sair}>
                    Sair do evento
                  </Botao>
                )}
              </>
            ) : partida.situacao === "ABERTA" ? (
              <Botao carregando={acao === "participar"} disabled={!!acao} onClick={participar}>
                Participar
              </Botao>
            ) : (
              <Botao disabled>{MOTIVO_SEM_PARTICIPAR[partida.situacao] ?? "Indisponível"}</Botao>
            )}

            <button
              type="button"
              onClick={copiarLink}
              className="text-sm font-semibold text-primary hover:underline"
              aria-live="polite"
            >
              {linkCopiado ? "Link copiado! ✓" : "Copiar link do evento"}
            </button>
          </section>

          <section className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda">
            <h2 className="mb-3 font-semibold">Participantes ({participantes.length})</h2>
            {erroParticipantes ? (
              <Alerta tipo="aviso">Não foi possível carregar os participantes: {erroParticipantes}</Alerta>
            ) : participantes.length === 0 ? (
              <p className="text-sm text-texto-suave">Ninguém confirmado ainda.</p>
            ) : (
              <ul className="flex flex-col gap-1">
                {participantes.map((p) => {
                  const souEu = p.id === usuario?.id;
                  return (
                    <li key={p.id}>
                      <Link
                        href={souEu ? "/perfil" : `/usuarios/${p.id}`}
                        className="flex items-center gap-3 rounded-xl p-2 transition hover:bg-fundo"
                      >
                        <Avatar nome={p.nome} fotoUrl={p.fotoUrl} />
                        <span className="min-w-0 flex-1">
                          <span className="block truncate font-medium">
                            {p.nome}
                            {souEu && <span className="text-texto-suave"> (você)</span>}
                          </span>
                          {p.nivelHabilidade !== undefined && (
                            <span className="block text-xs text-texto-suave">
                              Nível {textoNivel(p.nivelHabilidade)}
                            </span>
                          )}
                        </span>
                        {p.criador && (
                          <span className="rounded-full bg-secondary/15 px-2 py-0.5 text-xs font-semibold text-secondary">
                            Organizador
                          </span>
                        )}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>
        </aside>

        <article className="flex flex-col gap-6 lg:col-span-2">
          {partida.descricao && (
            <section className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda">
              <h2 className="mb-2 font-semibold">Sobre o evento</h2>
              <p className="whitespace-pre-line text-texto-suave">{partida.descricao}</p>
            </section>
          )}

          <section className="grid gap-4 rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:grid-cols-2">
            <div>
              <h2 className="text-sm font-medium text-texto-suave">Quando</h2>
              <p className="font-semibold first-letter:uppercase">
                {formatarDiaLongo(partida.dataHoraInicio)}
              </p>
              <p>
                {formatarHora(partida.dataHoraInicio)} às {formatarHora(partida.dataHoraFim)}
                {partida.duracaoMinutos !== undefined && (
                  <span className="text-texto-suave"> · {formatarDuracao(partida.duracaoMinutos)}</span>
                )}
              </p>
            </div>

            <div>
              <h2 className="text-sm font-medium text-texto-suave">Onde</h2>
              <p className="font-semibold">{partida.local.nome}</p>
              {partida.local.endereco && <p>{partida.local.endereco}</p>}
              <p className="text-sm text-texto-suave">
                {partida.local.tipo && TIPOS_LOCAL[partida.local.tipo]}
                {partida.distanciaKm !== undefined && ` · a ${formatarDistancia(partida.distanciaKm)}`}
              </p>
              {mapa && (
                <a
                  href={mapa}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm font-semibold text-primary hover:underline"
                >
                  Ver no mapa ↗
                </a>
              )}
            </div>

            {partida.quadra && (
              <div>
                <h2 className="text-sm font-medium text-texto-suave">Quadra</h2>
                <p className="font-semibold">{partida.quadra.nome}</p>
                {partida.quadra.precoHora !== undefined && (
                  <p className="text-sm text-texto-suave">
                    {formatarPreco(partida.quadra.precoHora)} por hora
                  </p>
                )}
              </div>
            )}
          </section>
        </article>
      </div>
    </main>
  );
}
