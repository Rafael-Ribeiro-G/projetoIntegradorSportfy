"use client";

// Ver Eventos: busca + filtros + grade de cards + "Carregar mais" (paginação da API).

import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoSelecao, CampoTexto } from "@/components/CampoTexto";
import { EventoCard, EventoCardEsqueleto } from "@/components/EventoCard";
import { TituloPagina } from "@/components/TituloPagina";
import { fimDoDia, inicioDoDia } from "@/lib/datas";
import { obterPosicao } from "@/lib/geolocalizacao";
import { TIPOS_LOCAL } from "@/lib/rotulos";
import { mensagemDeErro } from "@/services/api";
import { listarEsportes } from "@/services/esportes";
import { listarPartidas } from "@/services/partidas";
import type { Esporte } from "@/types/esporte";
import type { TipoLocal } from "@/types/local";
import type { FiltrosPartidas, Partida } from "@/types/partida";

const TAMANHO_PAGINA = 12;
const RAIOS_KM = [5, 10, 25, 50];

/** Filtros como ficam na tela (texto dos inputs). */
interface Filtros {
  texto: string;
  esporteId: string;
  tipoLocal: TipoLocal | "";
  dataInicio: string; // "2026-10-25" (input type="date")
  dataFim: string;
  raioKm: string; // "" = sem filtro de distância
  latitude?: number;
  longitude?: number;
}

const FILTROS_INICIAIS: Filtros = {
  texto: "",
  esporteId: "",
  tipoLocal: "",
  dataInicio: "",
  dataFim: "",
  raioKm: "",
};

/** Converte os filtros da tela no formato que a API espera. */
function paraApi(f: Filtros, pagina: number): FiltrosPartidas {
  const pertoDeMim = f.raioKm !== "" && f.latitude !== undefined && f.longitude !== undefined;
  return {
    texto: f.texto.trim() || undefined,
    esporteId: f.esporteId ? Number(f.esporteId) : undefined,
    tipoLocal: f.tipoLocal || undefined,
    dataInicio: f.dataInicio ? inicioDoDia(f.dataInicio) : undefined,
    dataFim: f.dataFim ? fimDoDia(f.dataFim) : undefined,
    latitude: pertoDeMim ? f.latitude : undefined,
    longitude: pertoDeMim ? f.longitude : undefined,
    raioKm: pertoDeMim ? Number(f.raioKm) : undefined,
    pagina,
    tamanho: TAMANHO_PAGINA,
  };
}

function quantosFiltrosAtivos(f: Filtros): number {
  return [f.texto.trim(), f.esporteId, f.tipoLocal, f.dataInicio, f.dataFim, f.raioKm].filter(Boolean)
    .length;
}

export default function EventosPage() {
  const [filtros, setFiltros] = useState<Filtros>(FILTROS_INICIAIS);
  const [textoDigitado, setTextoDigitado] = useState("");
  const [mostrarFiltros, setMostrarFiltros] = useState(false); // só no celular

  const [partidas, setPartidas] = useState<Partida[]>([]);
  const [pagina, setPagina] = useState(0);
  const [ultima, setUltima] = useState(true);
  const [total, setTotal] = useState(0);

  const [carregando, setCarregando] = useState(true);
  const [carregandoMais, setCarregandoMais] = useState(false);
  const [erro, setErro] = useState("");
  const [erroMais, setErroMais] = useState("");

  const [esportes, setEsportes] = useState<Esporte[]>([]);
  const [localizando, setLocalizando] = useState(false);
  const [avisoLocal, setAvisoLocal] = useState("");

  // Cada busca ganha um número; se chegar a resposta de uma busca antiga, ignoramos.
  const ultimaBusca = useRef(0);

  /** Faz a busca; o resultado é aplicado no .then() (nunca de forma síncrona). */
  const buscar = useCallback((f: Filtros, paginaDesejada: number) => {
    const numero = ++ultimaBusca.current;
    const atual = () => numero === ultimaBusca.current;

    listarPartidas(paraApi(f, paginaDesejada))
      .then((resposta) => {
        if (!atual()) return;
        setPartidas((atuais) =>
          paginaDesejada === 0 ? resposta.conteudo : [...atuais, ...resposta.conteudo],
        );
        setPagina(resposta.pagina);
        setUltima(resposta.ultima);
        setTotal(resposta.totalElementos);
      })
      .catch((e) => {
        if (!atual()) return;
        if (paginaDesejada === 0) setErro(mensagemDeErro(e));
        else setErroMais(mensagemDeErro(e));
      })
      .finally(() => {
        if (!atual()) return;
        setCarregando(false);
        setCarregandoMais(false);
      });
  }, []);

  // Primeira busca (sem filtros) e lista de esportes para o filtro.
  useEffect(() => {
    buscar(FILTROS_INICIAIS, 0);
    listarEsportes()
      .then(setEsportes)
      .catch(() => setEsportes([])); // sem esportes, o filtro só fica vazio
    // Ao sair da tela, invalida a busca em andamento.
    const contador = ultimaBusca;
    return () => {
      contador.current++;
    };
  }, [buscar]);

  /** Troca os filtros e busca de novo a partir da página 0. */
  function aplicar(novos: Filtros) {
    setFiltros(novos);
    setCarregando(true);
    setErro("");
    setErroMais("");
    buscar(novos, 0);
  }

  function aoBuscarTexto(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    aplicar({ ...filtros, texto: textoDigitado });
  }

  function carregarMais() {
    setCarregandoMais(true);
    setErroMais("");
    buscar(filtros, pagina + 1);
  }

  async function aoMudarRaio(raio: string) {
    setAvisoLocal("");
    if (!raio) {
      aplicar({ ...filtros, raioKm: "" });
      return;
    }
    // Já temos a posição? Só troca o raio.
    if (filtros.latitude !== undefined && filtros.longitude !== undefined) {
      aplicar({ ...filtros, raioKm: raio });
      return;
    }
    setLocalizando(true);
    const posicao = await obterPosicao();
    setLocalizando(false);
    if (!posicao) {
      setAvisoLocal(
        "Não conseguimos sua localização. Permita o acesso à localização no navegador para filtrar por distância.",
      );
      return;
    }
    aplicar({ ...filtros, raioKm: raio, ...posicao });
  }

  function limparFiltros() {
    setTextoDigitado("");
    setAvisoLocal("");
    // Mantém a posição já obtida, para não pedir de novo.
    aplicar({ ...FILTROS_INICIAIS, latitude: filtros.latitude, longitude: filtros.longitude });
  }

  const ativos = quantosFiltrosAtivos(filtros);

  return (
    <main>
      <TituloPagina titulo="Eventos" subtitulo="Encontre um evento para participar" />

      {/* Busca por texto */}
      <form onSubmit={aoBuscarTexto} role="search" className="flex gap-2">
        <label htmlFor="busca" className="sr-only">
          Buscar eventos
        </label>
        <input
          id="busca"
          type="search"
          value={textoDigitado}
          onChange={(e) => setTextoDigitado(e.target.value)}
          placeholder="Buscar por título, local, endereço ou quadra"
          className="min-w-0 flex-1 rounded-xl border border-borda bg-superficie px-4 py-3 outline-none focus:border-primary focus:ring-2 focus:ring-primary-light"
        />
        <Botao type="submit" className="w-auto! px-6">
          Buscar
        </Botao>
      </form>

      {/* Botão de filtros (só no celular) */}
      <button
        type="button"
        onClick={() => setMostrarFiltros((v) => !v)}
        aria-expanded={mostrarFiltros}
        aria-controls="painel-filtros"
        className="mt-3 rounded-lg border border-borda bg-superficie px-4 py-2 text-sm font-medium md:hidden"
      >
        {mostrarFiltros ? "Esconder filtros" : "Filtros"}
        {ativos > 0 && ` (${ativos})`}
      </button>

      {/* Filtros */}
      <section
        id="painel-filtros"
        aria-label="Filtros"
        className={`${mostrarFiltros ? "grid" : "hidden"} mt-4 gap-4 rounded-2xl bg-superficie p-4 shadow-sm ring-1 ring-borda sm:grid-cols-2 md:grid md:grid-cols-3 lg:grid-cols-5`}
      >
        <CampoSelecao
          id="filtro-esporte"
          label="Esporte"
          value={filtros.esporteId}
          onChange={(e) => aplicar({ ...filtros, esporteId: e.target.value })}
        >
          <option value="">Todos</option>
          {esportes.map((esp) => (
            <option key={esp.id} value={esp.id}>
              {esp.nome}
            </option>
          ))}
        </CampoSelecao>

        <CampoSelecao
          id="filtro-tipo"
          label="Tipo de local"
          value={filtros.tipoLocal}
          onChange={(e) => aplicar({ ...filtros, tipoLocal: e.target.value as TipoLocal | "" })}
        >
          <option value="">Todos</option>
          {Object.entries(TIPOS_LOCAL).map(([valor, texto]) => (
            <option key={valor} value={valor}>
              {texto}
            </option>
          ))}
        </CampoSelecao>

        <CampoTexto
          id="filtro-data-inicio"
          label="A partir de"
          type="date"
          value={filtros.dataInicio}
          max={filtros.dataFim || undefined}
          onChange={(e) => aplicar({ ...filtros, dataInicio: e.target.value })}
        />

        <CampoTexto
          id="filtro-data-fim"
          label="Até"
          type="date"
          value={filtros.dataFim}
          min={filtros.dataInicio || undefined}
          onChange={(e) => aplicar({ ...filtros, dataFim: e.target.value })}
        />

        <CampoSelecao
          id="filtro-raio"
          label="Distância"
          value={filtros.raioKm}
          disabled={localizando}
          dica={localizando ? "Obtendo sua localização..." : undefined}
          onChange={(e) => aoMudarRaio(e.target.value)}
        >
          <option value="">Qualquer distância</option>
          {RAIOS_KM.map((km) => (
            <option key={km} value={km}>
              Até {km} km de mim
            </option>
          ))}
        </CampoSelecao>

        {ativos > 0 && (
          <div className="sm:col-span-2 md:col-span-3 lg:col-span-5">
            <button
              type="button"
              onClick={limparFiltros}
              className="text-sm font-semibold text-primary hover:underline"
            >
              Limpar filtros
            </button>
          </div>
        )}
      </section>

      {avisoLocal && (
        <div className="mt-4">
          <Alerta tipo="aviso">{avisoLocal}</Alerta>
        </div>
      )}

      {/* Resultados */}
      <section aria-label="Resultados" aria-busy={carregando} className="mt-6">
        {carregando ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, i) => (
              <EventoCardEsqueleto key={i} />
            ))}
          </div>
        ) : erro ? (
          <div className="flex flex-col items-start gap-3">
            <Alerta>Não foi possível carregar os eventos: {erro}</Alerta>
            <Botao variante="secundario" className="w-auto! px-6" onClick={() => aplicar(filtros)}>
              Tentar de novo
            </Botao>
          </div>
        ) : partidas.length === 0 ? (
          <div className="rounded-2xl bg-superficie p-10 text-center shadow-sm ring-1 ring-borda">
            <p className="text-lg font-semibold">Nenhum evento encontrado</p>
            <p className="mt-1 text-texto-suave">
              {ativos > 0
                ? "Tente mudar ou limpar os filtros."
                : "Ainda não há eventos por aqui. Que tal criar o primeiro?"}
            </p>
          </div>
        ) : (
          <>
            <p className="mb-4 text-sm text-texto-suave" aria-live="polite">
              {total === 1 ? "1 evento encontrado" : `${total} eventos encontrados`}
            </p>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {partidas.map((p) => (
                <EventoCard key={p.id} partida={p} />
              ))}
            </div>

            <div className="mt-8 flex flex-col items-center gap-3">
              {erroMais && <Alerta>{erroMais}</Alerta>}
              {!ultima && (
                <Botao
                  variante="secundario"
                  className="w-auto! px-8"
                  carregando={carregandoMais}
                  onClick={carregarMais}
                >
                  Carregar mais
                </Botao>
              )}
            </div>
          </>
        )}
      </section>
    </main>
  );
}
