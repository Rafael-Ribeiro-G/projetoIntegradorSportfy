"use client";

// SportIA: botão flutuante + painel de chat. Fica no layout logado, então a conversa
// continua enquanto a pessoa navega entre as páginas.

import { Fragment, useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { ApiError, mensagemDeErro } from "@/services/api";
import { MAX_PERGUNTA, perguntarSportIA } from "@/services/chatbot";

interface Mensagem {
  id: number;
  autor: "eu" | "ia";
  texto: string;
  erro?: boolean;
}

const BOAS_VINDAS: Mensagem = {
  id: 0,
  autor: "ia",
  texto:
    "Oi! Eu sou o SportIA 🤖⚽\nPosso te ajudar a encontrar eventos, explicar como criar ou participar de um, e tirar dúvidas sobre o Sportfy. O que você quer saber?",
};

const SUGESTOES = [
  "Como participar de um evento?",
  "Como criar um evento?",
  "Tem algum evento perto de mim?",
];

/** Traduz o erro da API para uma mensagem amigável (tabela de erros do CLAUDE.md). */
function mensagemDoErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    if (erro.status === 429) return "Muitas perguntas, aguarde um pouco antes de perguntar de novo.";
    if ([408, 502, 503, 504].includes(erro.status)) {
      return "SportIA indisponível no momento. Tente de novo daqui a pouco.";
    }
  }
  return mensagemDeErro(erro);
}

/** Mostra **negrito** (comum nas respostas da IA) sem interpretar HTML. */
function TextoFormatado({ texto }: { texto: string }) {
  const partes = texto.split(/(\*\*[^*]+\*\*)/g);
  return (
    <>
      {partes.map((parte, i) =>
        parte.startsWith("**") && parte.endsWith("**") && parte.length > 4 ? (
          <strong key={i}>{parte.slice(2, -2)}</strong>
        ) : (
          <Fragment key={i}>{parte}</Fragment>
        ),
      )}
    </>
  );
}

export function ChatSportIA() {
  const [aberto, setAberto] = useState(false);
  const [mensagens, setMensagens] = useState<Mensagem[]>([BOAS_VINDAS]);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);

  const proximoId = useRef(1);
  const listaRef = useRef<HTMLDivElement>(null);
  const campoRef = useRef<HTMLTextAreaElement>(null);
  const botaoRef = useRef<HTMLButtonElement>(null);
  const controleRef = useRef<AbortController | null>(null);

  // Rola para a última mensagem sempre que chega uma nova (ou aparece o "digitando...").
  useEffect(() => {
    const lista = listaRef.current;
    if (lista) lista.scrollTo({ top: lista.scrollHeight, behavior: "smooth" });
  }, [mensagens, enviando, aberto]);

  // Ao fechar, o foco volta para o botão flutuante. Isso precisa acontecer DEPOIS da tela
  // atualizar: no celular o botão fica escondido enquanto o painel está aberto.
  const devolverFocoRef = useRef(false);
  function fechar() {
    devolverFocoRef.current = true;
    setAberto(false);
  }

  useEffect(() => {
    if (!aberto) {
      if (devolverFocoRef.current) botaoRef.current?.focus();
      devolverFocoRef.current = false;
      return;
    }
    // Ao abrir, coloca o cursor no campo. Esc fecha.
    campoRef.current?.focus();
    const aoTeclar = (e: globalThis.KeyboardEvent) => {
      if (e.key === "Escape") fechar();
    };
    window.addEventListener("keydown", aoTeclar);
    return () => window.removeEventListener("keydown", aoTeclar);
  }, [aberto]);

  // Ao sair da área logada (logout), cancela a pergunta em andamento.
  useEffect(() => () => controleRef.current?.abort(), []);

  function adicionar(autor: Mensagem["autor"], conteudo: string, erro = false) {
    const nova = { id: proximoId.current++, autor, texto: conteudo, erro };
    setMensagens((atuais) => [...atuais, nova]);
  }

  async function enviar(pergunta: string) {
    const limpa = pergunta.trim();
    if (!limpa || enviando || limpa.length > MAX_PERGUNTA) return;

    adicionar("eu", limpa);
    setTexto("");
    setEnviando(true);

    const controle = new AbortController();
    controleRef.current = controle;
    try {
      const { resposta } = await perguntarSportIA(limpa, controle.signal);
      adicionar("ia", resposta?.trim() || "Não consegui responder agora. Tente perguntar de outro jeito.");
    } catch (erro) {
      if (erro instanceof DOMException && erro.name === "AbortError") return; // cancelado de propósito
      adicionar("ia", mensagemDoErro(erro), true);
    } finally {
      if (controleRef.current === controle) controleRef.current = null;
      setEnviando(false);
      campoRef.current?.focus();
    }
  }

  function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    enviar(texto);
  }

  // Enter envia; Shift+Enter quebra a linha.
  function aoTeclarNoCampo(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      enviar(texto);
    }
  }

  function limparConversa() {
    controleRef.current?.abort();
    controleRef.current = null;
    setEnviando(false);
    setMensagens([BOAS_VINDAS]);
  }

  const passouDoLimite = texto.length > MAX_PERGUNTA;
  const soBoasVindas = mensagens.length === 1;

  return (
    <>
      {/* Painel */}
      {aberto && (
        <section
          id="painel-sportia"
          role="dialog"
          aria-label="Chat com o SportIA"
          className="fixed inset-0 z-50 flex flex-col bg-superficie shadow-2xl sm:inset-auto sm:bottom-24 sm:right-6 sm:h-[min(560px,calc(100dvh-8rem))] sm:w-[380px] sm:overflow-hidden sm:rounded-2xl sm:ring-1 sm:ring-borda"
        >
          {/* Cabeçalho */}
          <header className="flex items-center justify-between gap-2 bg-gradient-to-br from-primary to-primary-dark px-4 py-3 text-white">
            <div className="flex items-center gap-3">
              <span aria-hidden className="flex h-9 w-9 items-center justify-center rounded-full bg-white/20 text-lg">
                🤖
              </span>
              <div>
                <h2 className="font-bold leading-tight">SportIA</h2>
                <p className="text-xs text-white/80">Assistente do Sportfy</p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              {!soBoasVindas && (
                <button
                  type="button"
                  onClick={limparConversa}
                  className="rounded-lg px-2 py-1 text-xs font-medium text-white/90 hover:bg-white/15"
                >
                  Limpar
                </button>
              )}
              <button
                type="button"
                onClick={fechar}
                aria-label="Fechar chat"
                className="rounded-lg p-1.5 hover:bg-white/15"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className="h-5 w-5" aria-hidden>
                  <path d="M6 6l12 12M18 6 6 18" />
                </svg>
              </button>
            </div>
          </header>

          {/* Mensagens */}
          <div
            ref={listaRef}
            aria-live="polite"
            aria-busy={enviando}
            className="flex flex-1 flex-col gap-3 overflow-y-auto bg-fundo p-4"
          >
            {mensagens.map((m) => (
              <div key={m.id} className={`flex ${m.autor === "eu" ? "justify-end" : "justify-start"}`}>
                <p
                  className={`max-w-[85%] whitespace-pre-line rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                    m.autor === "eu"
                      ? "rounded-br-md bg-primary text-white"
                      : m.erro
                        ? "rounded-bl-md bg-erro/10 text-erro ring-1 ring-erro/20"
                        : "rounded-bl-md bg-superficie text-texto shadow-sm ring-1 ring-borda"
                  }`}
                >
                  <span className="sr-only">{m.autor === "eu" ? "Você: " : "SportIA: "}</span>
                  {m.autor === "ia" ? <TextoFormatado texto={m.texto} /> : m.texto}
                </p>
              </div>
            ))}

            {/* Sugestões, só no começo da conversa */}
            {soBoasVindas && !enviando && (
              <div className="flex flex-wrap gap-2">
                {SUGESTOES.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => enviar(s)}
                    className="rounded-full border border-primary/40 bg-superficie px-3 py-1.5 text-xs font-medium text-primary hover:bg-primary-light"
                  >
                    {s}
                  </button>
                ))}
              </div>
            )}

            {enviando && (
              <div className="flex justify-start" role="status">
                <p className="flex items-center gap-2 rounded-2xl rounded-bl-md bg-superficie px-4 py-2.5 text-sm text-texto-suave shadow-sm ring-1 ring-borda">
                  <span aria-hidden className="flex gap-1">
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-texto-suave [animation-delay:-0.3s]" />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-texto-suave [animation-delay:-0.15s]" />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-texto-suave" />
                  </span>
                  SportIA está digitando...
                </p>
              </div>
            )}
          </div>

          {/* Campo de pergunta */}
          <form onSubmit={aoEnviar} className="border-t border-borda bg-superficie p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
            <div className="flex items-end gap-2">
              <label htmlFor="pergunta-sportia" className="sr-only">
                Sua pergunta para o SportIA
              </label>
              <textarea
                id="pergunta-sportia"
                ref={campoRef}
                rows={1}
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                onKeyDown={aoTeclarNoCampo}
                placeholder="Pergunte algo..."
                aria-invalid={passouDoLimite}
                aria-describedby="contador-sportia"
                className={`max-h-32 min-h-11 flex-1 resize-none rounded-xl border bg-superficie px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-primary-light ${
                  passouDoLimite ? "border-erro" : "border-borda focus:border-primary"
                }`}
              />
              <button
                type="submit"
                disabled={enviando || !texto.trim() || passouDoLimite}
                aria-label="Enviar pergunta"
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary text-white transition hover:bg-primary-dark disabled:opacity-50"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5" aria-hidden>
                  <path d="M5 12h14M13 6l6 6-6 6" />
                </svg>
              </button>
            </div>
            <p
              id="contador-sportia"
              className={`mt-1 text-right text-xs ${passouDoLimite ? "text-erro" : "text-texto-suave"}`}
            >
              {passouDoLimite ? "Pergunta muito longa · " : ""}
              {texto.length}/{MAX_PERGUNTA}
            </p>
          </form>
        </section>
      )}

      {/* Botão flutuante (no celular, some enquanto o painel ocupa a tela) */}
      <button
        ref={botaoRef}
        type="button"
        onClick={() => setAberto((a) => !a)}
        aria-expanded={aberto}
        aria-controls="painel-sportia"
        aria-label={aberto ? "Fechar o SportIA" : "Abrir o SportIA, assistente do Sportfy"}
        className={`fixed bottom-5 right-5 z-40 h-14 items-center gap-2 rounded-full bg-primary px-4 text-white shadow-lg transition hover:bg-primary-dark sm:bottom-6 sm:right-6 ${
          aberto ? "hidden sm:flex" : "flex"
        }`}
      >
        {aberto ? (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" className="h-6 w-6" aria-hidden>
            <path d="M6 6l12 12M18 6 6 18" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" className="h-6 w-6" aria-hidden>
            <path d="M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z" />
          </svg>
        )}
        <span className="text-sm font-semibold">SportIA</span>
      </button>
    </>
  );
}
