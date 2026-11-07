"use client";

// Formulário usado por Criar evento e Editar evento.
// Passos: 1) local → 2) quadra ou esporte → 3) detalhes (título, descrição, capa) → 4) quando e vagas.

import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoAreaTexto, CampoSelecao, CampoTexto } from "@/components/CampoTexto";
import { SeletorLocal } from "@/components/SeletorLocal";
import {
  agoraDatetimeLocal,
  apiParaDatetimeLocal,
  datetimeLocalParaApi,
  formatarDuracao,
  minutosEntre,
  somarMinutos,
} from "@/lib/datas";
import { formatarPreco } from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import { listarEsportes } from "@/services/esportes";
import { listarQuadras } from "@/services/quadras";
import { enviarImagem, TIPOS_IMAGEM_ACEITOS, validarImagem } from "@/services/uploads";
import type { Esporte } from "@/types/esporte";
import type { LocalResumo, Quadra } from "@/types/local";
import type { PartidaRequest } from "@/types/partida";

const MAX_TITULO = 100;
const MAX_DESCRICAO = 1000;
const DURACAO_MIN = 15; // minutos
const DURACAO_MAX = 12 * 60;

/** Valores para abrir o formulário já preenchido (tela Editar). Datas no formato da API. */
export interface DadosIniciaisEvento {
  local: LocalResumo;
  quadraId?: string;
  esporteId?: number;
  titulo?: string;
  descricao?: string;
  imagemUrl?: string;
  dataHoraInicio: string;
  dataHoraFim: string;
  maxJogadores?: number;
  /** Para não deixar o limite ficar menor que o número de confirmados. */
  participantesConfirmados?: number;
}

interface Props {
  inicial?: DadosIniciaisEvento;
  textoBotao: string;
  /** Envia para a API. Se lançar ApiError, o formulário mostra os erros. */
  aoEnviar: (dados: PartidaRequest) => Promise<void>;
}

type Erros = Record<string, string>;

/** Cartão numerado de cada passo. */
function Passo({ numero, titulo, children }: { numero: number; titulo: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:p-6">
      <h2 className="mb-4 flex items-center gap-3 text-lg font-semibold">
        <span className="flex h-7 w-7 items-center justify-center rounded-full bg-primary text-sm text-white">
          {numero}
        </span>
        {titulo}
      </h2>
      <div className="flex flex-col gap-4">{children}</div>
    </section>
  );
}

export function FormularioEvento({ inicial, textoBotao, aoEnviar }: Props) {
  // Passo 1 e 2
  const [local, setLocal] = useState<LocalResumo | null>(inicial?.local ?? null);
  const [quadras, setQuadras] = useState<Quadra[]>([]);
  const [carregandoQuadras, setCarregandoQuadras] = useState(!!inicial?.local);
  const [erroQuadras, setErroQuadras] = useState("");
  const [quadraId, setQuadraId] = useState(inicial?.quadraId ?? "");
  const [esporteId, setEsporteId] = useState(inicial?.esporteId ? String(inicial.esporteId) : "");
  const [esportes, setEsportes] = useState<Esporte[]>([]);

  // Passo 3
  const [titulo, setTitulo] = useState(inicial?.titulo ?? "");
  const [descricao, setDescricao] = useState(inicial?.descricao ?? "");
  const [capaUrl, setCapaUrl] = useState<string | null>(inicial?.imagemUrl ?? null); // já está no servidor
  const [arquivoCapa, setArquivoCapa] = useState<File | null>(null); // escolhida agora, ainda não enviada
  const [previewCapa, setPreviewCapa] = useState<string | null>(null);
  const previewRef = useRef<string | null>(null);

  // Passo 4
  const [inicio, setInicio] = useState(inicial ? apiParaDatetimeLocal(inicial.dataHoraInicio) : "");
  const [fim, setFim] = useState(inicial ? apiParaDatetimeLocal(inicial.dataHoraFim) : "");
  const [maxJogadores, setMaxJogadores] = useState(
    inicial?.maxJogadores !== undefined ? String(inicial.maxJogadores) : "",
  );

  const [erros, setErros] = useState<Erros>({});
  const [erroGeral, setErroGeral] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Esportes (para quando o evento não usa quadra).
  useEffect(() => {
    let ativo = true;
    listarEsportes()
      .then((lista) => ativo && setEsportes(lista))
      .catch(() => ativo && setEsportes([]));
    return () => {
      ativo = false;
    };
  }, []);

  // Quadras do local escolhido.
  const localId = local?.id;
  useEffect(() => {
    if (!localId) return;
    let ativo = true;
    listarQuadras({ localId })
      .then((lista) => {
        if (!ativo) return;
        setQuadras(lista);
        setErroQuadras("");
      })
      .catch((e) => ativo && setErroQuadras(mensagemDeErro(e)))
      .finally(() => ativo && setCarregandoQuadras(false));
    return () => {
      ativo = false;
    };
  }, [localId]);

  // Libera a URL temporária da prévia ao sair da tela.
  useEffect(
    () => () => {
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    },
    [],
  );

  const quadraEscolhida = quadras.find((q) => q.id === quadraId);
  const esporteEfetivo =
    quadraEscolhida?.esporte ?? esportes.find((e) => String(e.id) === esporteId);
  const duracao = inicio && fim ? minutosEntre(inicio, fim) : null;

  function limparErro(campo: string) {
    setErros((atual) => {
      if (!(campo in atual)) return atual;
      const resto = { ...atual };
      delete resto[campo];
      return resto;
    });
  }

  function escolherLocal(novo: LocalResumo | null) {
    setLocal(novo);
    setQuadraId("");
    setQuadras([]);
    setErroQuadras("");
    setCarregandoQuadras(!!novo);
    limparErro("localId");
    limparErro("quadraId");
  }

  function aoMudarInicio(valor: string) {
    setInicio(valor);
    limparErro("dataHoraInicio");
    // Facilita: se o fim estiver vazio ou antes do início, sugere 1 hora de duração.
    if (valor && (!fim || minutosEntre(valor, fim) <= 0)) {
      setFim(somarMinutos(valor, 60));
      limparErro("dataHoraFim");
    }
  }

  function aoEscolherCapa(e: ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    e.target.value = ""; // permite escolher o mesmo arquivo de novo
    if (!arquivo) return;

    const problema = validarImagem(arquivo);
    if (problema) {
      setErros((atual) => ({ ...atual, imagemUrl: problema }));
      return;
    }
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = URL.createObjectURL(arquivo);
    setArquivoCapa(arquivo);
    setPreviewCapa(previewRef.current);
    limparErro("imagemUrl");
  }

  function removerCapa() {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = null;
    setArquivoCapa(null);
    setPreviewCapa(null);
    setCapaUrl(null);
  }

  function validar(): Erros {
    const novos: Erros = {};
    const agora = agoraDatetimeLocal();

    if (!local) novos.localId = "Escolha ou cadastre o local.";
    if (local && !quadraId && !esporteId) novos.esporteId = "Escolha o esporte.";

    if (titulo.trim().length > MAX_TITULO) novos.titulo = `Use no máximo ${MAX_TITULO} caracteres.`;
    if (descricao.trim().length > MAX_DESCRICAO) {
      novos.descricao = `Use no máximo ${MAX_DESCRICAO} caracteres.`;
    }

    if (!inicio) novos.dataHoraInicio = "Informe quando começa.";
    else if (inicio <= agora) novos.dataHoraInicio = "O início precisa ser no futuro.";
    else if (inicio > somarMinutos(agora, 365 * 24 * 60)) {
      novos.dataHoraInicio = "No máximo 1 ano de antecedência.";
    }

    if (!fim) novos.dataHoraFim = "Informe quando termina.";
    else if (inicio) {
      const d = minutosEntre(inicio, fim);
      if (d <= 0) novos.dataHoraFim = "O fim precisa ser depois do início.";
      else if (d < DURACAO_MIN) novos.dataHoraFim = "O evento precisa durar pelo menos 15 minutos.";
      else if (d > DURACAO_MAX) novos.dataHoraFim = "O evento pode durar no máximo 12 horas.";
    }

    if (maxJogadores) {
      const n = Number(maxJogadores);
      const confirmados = inicial?.participantesConfirmados ?? 0;
      if (!Number.isInteger(n) || n < 2 || n > 100) {
        novos.maxJogadores = "Use um número de 2 a 100.";
      } else if (n < confirmados) {
        novos.maxJogadores = `Já há ${confirmados} confirmados; o limite não pode ser menor.`;
      }
    }
    return novos;
  }

  async function aoSubmeter(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral("");

    const novos = validar();
    setErros(novos);
    if (Object.keys(novos).length > 0 || !local) {
      setErroGeral("Confira os campos destacados.");
      return;
    }

    setEnviando(true);
    try {
      // 1) Capa nova? Envia primeiro e usa a url devolvida.
      let imagemUrl = capaUrl;
      if (arquivoCapa) {
        try {
          imagemUrl = (await enviarImagem(arquivoCapa)).url;
          // Guarda a url: se o passo 2 falhar, não precisa enviar a imagem de novo.
          setCapaUrl(imagemUrl);
          setArquivoCapa(null);
        } catch (erro) {
          setErros({ imagemUrl: mensagemDeErro(erro) });
          setErroGeral("Não foi possível enviar a capa. Tente outra imagem ou remova a capa.");
          return;
        }
      }

      // 2) Monta o corpo. Opcionais vazios vão como null (no PUT, isso apaga o valor antigo).
      const base = {
        titulo: titulo.trim() || null,
        descricao: descricao.trim() || null,
        imagemUrl,
        dataHoraInicio: datetimeLocalParaApi(inicio),
        dataHoraFim: datetimeLocalParaApi(fim),
        maxJogadores: maxJogadores ? Number(maxJogadores) : null,
      };
      const dados: PartidaRequest = quadraId
        ? { ...base, quadraId } // local e esporte vêm da quadra
        : { ...base, localId: local.id, esporteId: Number(esporteId) };

      await aoEnviar(dados);
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 400 && erro.campos.length > 0) {
        setErros(erro.errosPorCampo());
        setErroGeral("Confira os campos destacados.");
      } else if (erro instanceof ApiError && erro.status === 409) {
        setErros({ dataHoraInicio: "A quadra já tem outro evento nesse horário." });
        setErroGeral(`${erro.mensagem} Escolha outro horário ou outra quadra.`);
      } else {
        setErroGeral(mensagemDeErro(erro));
      }
    } finally {
      setEnviando(false);
    }
  }

  const capaParaMostrar = previewCapa ?? capaUrl;

  return (
    <div className="flex flex-col gap-6">
      {/* Passo 1 fica fora do <form> principal porque tem o próprio formulário de "novo local". */}
      <Passo numero={1} titulo="Onde vai ser?">
        <SeletorLocal valor={local} aoEscolher={escolherLocal} erro={erros.localId} />
      </Passo>

      <form onSubmit={aoSubmeter} noValidate className="flex flex-col gap-6">
        <Passo numero={2} titulo="Qual esporte?">
          {!local ? (
            <p className="text-sm text-texto-suave">Escolha o local primeiro.</p>
          ) : carregandoQuadras ? (
            <p className="text-sm text-texto-suave">Carregando quadras do local...</p>
          ) : (
            <>
              {erroQuadras && (
                <Alerta tipo="aviso">Não foi possível carregar as quadras: {erroQuadras}</Alerta>
              )}

              {quadras.length > 0 && (
                <fieldset className="flex flex-col gap-2">
                  <legend className="mb-2 text-sm font-medium">
                    Este local tem quadras. Vai usar uma delas?
                  </legend>
                  <OpcaoQuadra
                    marcada={quadraId === ""}
                    aoMarcar={() => setQuadraId("")}
                    titulo="Não, sem quadra específica"
                    detalhe="Ex.: corrida, caminhada, treino livre"
                  />
                  {quadras.map((q) => (
                    <OpcaoQuadra
                      key={q.id}
                      marcada={quadraId === q.id}
                      aoMarcar={() => {
                        setQuadraId(q.id);
                        limparErro("esporteId");
                        limparErro("quadraId");
                      }}
                      titulo={q.nome}
                      detalhe={[
                        q.esporte?.nome,
                        q.precoHora !== undefined && `${formatarPreco(q.precoHora)}/hora`,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    />
                  ))}
                  {erros.quadraId && <p className="text-sm text-erro">{erros.quadraId}</p>}
                </fieldset>
              )}

              {quadraEscolhida ? (
                <p className="text-sm text-texto-suave">
                  Esporte: <strong className="text-texto">{quadraEscolhida.esporte?.nome}</strong>{" "}
                  (definido pela quadra)
                </p>
              ) : (
                <CampoSelecao
                  id="esporteId"
                  label="Esporte *"
                  value={esporteId}
                  onChange={(e) => {
                    setEsporteId(e.target.value);
                    limparErro("esporteId");
                  }}
                  erro={erros.esporteId}
                >
                  <option value="">Escolha...</option>
                  {esportes.map((esp) => (
                    <option key={esp.id} value={esp.id}>
                      {esp.nome}
                    </option>
                  ))}
                </CampoSelecao>
              )}
            </>
          )}
        </Passo>

        <Passo numero={3} titulo="Detalhes">
          <CampoTexto
            id="titulo"
            label="Título"
            placeholder={
              esporteEfetivo && local ? `Ex.: ${esporteEfetivo.nome} no ${local.nome}` : "Ex.: Fut de quarta"
            }
            maxLength={MAX_TITULO}
            value={titulo}
            onChange={(e) => {
              setTitulo(e.target.value);
              limparErro("titulo");
            }}
            erro={erros.titulo}
            dica={`Opcional · ${titulo.length}/${MAX_TITULO}`}
          />
          <CampoAreaTexto
            id="descricao"
            label="Descrição"
            rows={4}
            placeholder="Nível, o que levar, ponto de encontro..."
            maxLength={MAX_DESCRICAO}
            value={descricao}
            onChange={(e) => {
              setDescricao(e.target.value);
              limparErro("descricao");
            }}
            erro={erros.descricao}
            dica={`Opcional · ${descricao.length}/${MAX_DESCRICAO}`}
          />

          <div className="flex flex-col gap-2">
            <span className="text-sm font-medium">Capa</span>
            {capaParaMostrar ? (
              <div className="relative aspect-21/9 overflow-hidden rounded-xl bg-fundo">
                {/* eslint-disable-next-line @next/next/no-img-element -- prévia local (blob:) ou imagem do servidor */}
                <img src={capaParaMostrar} alt="Prévia da capa do evento" className="h-full w-full object-cover" />
              </div>
            ) : (
              <p className="text-sm text-texto-suave">
                Sem capa, o evento usa uma capa verde com o nome do esporte.
              </p>
            )}
            <div className="flex flex-wrap items-center gap-3">
              <label
                htmlFor="capa"
                className="cursor-pointer rounded-lg bg-primary-light px-3 py-2 text-sm font-semibold text-primary has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary"
              >
                {capaParaMostrar ? "Trocar imagem" : "Escolher imagem"}
                <input
                  id="capa"
                  type="file"
                  accept={TIPOS_IMAGEM_ACEITOS.join(",")}
                  onChange={aoEscolherCapa}
                  className="sr-only"
                />
              </label>
              {capaParaMostrar && (
                <button type="button" onClick={removerCapa} className="text-sm text-texto-suave underline">
                  Remover capa
                </button>
              )}
            </div>
            {erros.imagemUrl ? (
              <p className="text-sm text-erro">{erros.imagemUrl}</p>
            ) : (
              <p className="text-sm text-texto-suave">JPG, PNG ou WEBP de até 5 MB.</p>
            )}
          </div>
        </Passo>

        <Passo numero={4} titulo="Quando e quantas pessoas?">
          <div className="grid items-start gap-4 sm:grid-cols-2">
            <CampoTexto
              id="dataHoraInicio"
              label="Começa em *"
              type="datetime-local"
              min={agoraDatetimeLocal()}
              value={inicio}
              onChange={(e) => aoMudarInicio(e.target.value)}
              erro={erros.dataHoraInicio}
            />
            <CampoTexto
              id="dataHoraFim"
              label="Termina em *"
              type="datetime-local"
              min={inicio || undefined}
              value={fim}
              onChange={(e) => {
                setFim(e.target.value);
                limparErro("dataHoraFim");
              }}
              erro={erros.dataHoraFim}
              dica={
                duracao !== null && duracao > 0
                  ? `Duração: ${formatarDuracao(duracao)} (de 15 min a 12 h)`
                  : "De 15 minutos a 12 horas."
              }
            />
          </div>
          <CampoTexto
            id="maxJogadores"
            label="Máximo de pessoas"
            type="number"
            inputMode="numeric"
            min={Math.max(2, inicial?.participantesConfirmados ?? 0)}
            max={100}
            placeholder={esporteEfetivo?.quantidadeJogadoresPadrao ? String(esporteEfetivo.quantidadeJogadoresPadrao) : "Sem limite"}
            value={maxJogadores}
            onChange={(e) => {
              setMaxJogadores(e.target.value);
              limparErro("maxJogadores");
            }}
            erro={erros.maxJogadores}
            dica={
              esporteEfetivo?.quantidadeJogadoresPadrao
                ? `Opcional (2 a 100). Em branco, usa o padrão de ${esporteEfetivo.nome}: ${esporteEfetivo.quantidadeJogadoresPadrao} pessoas.`
                : "Opcional (2 a 100). Em branco, usa o padrão do esporte (pode ser sem limite)."
            }
          />
        </Passo>

        {erroGeral && <Alerta>{erroGeral}</Alerta>}

        <Botao type="submit" carregando={enviando} className="sm:w-auto! sm:self-end sm:px-10">
          {textoBotao}
        </Botao>
      </form>
    </div>
  );
}

/** Uma opção (rádio) da lista de quadras. */
function OpcaoQuadra({
  marcada,
  aoMarcar,
  titulo,
  detalhe,
}: {
  marcada: boolean;
  aoMarcar: () => void;
  titulo: string;
  detalhe?: string;
}) {
  return (
    <label
      className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3 transition has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary ${
        marcada ? "border-primary bg-primary-light/40" : "border-borda hover:border-primary"
      }`}
    >
      <input type="radio" name="quadra" checked={marcada} onChange={aoMarcar} className="mt-1 accent-primary" />
      <span>
        <span className="block font-medium">{titulo}</span>
        {detalhe && <span className="block text-sm text-texto-suave">{detalhe}</span>}
      </span>
    </label>
  );
}
