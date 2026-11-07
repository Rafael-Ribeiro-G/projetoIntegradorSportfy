"use client";

// Passo "Local" do Criar/Editar evento: buscar um local existente ou cadastrar um novo.

import { useEffect, useState, type FormEvent } from "react";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoSelecao, CampoTexto } from "@/components/CampoTexto";
import { obterPosicao, type Posicao } from "@/lib/geolocalizacao";
import { TIPOS_LOCAL } from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import { criarLocal, listarLocais } from "@/services/locais";
import type { Local, LocalResumo, TipoLocal } from "@/types/local";

const MAX_RESULTADOS = 8;

interface Props {
  valor: LocalResumo | null;
  aoEscolher: (local: LocalResumo | null) => void;
  erro?: string;
}

export function SeletorLocal({ valor, aoEscolher, erro }: Props) {
  const [modo, setModo] = useState<"buscar" | "novo">("buscar");

  // Local escolhido: mostra o resumo + "Trocar".
  if (valor) {
    return (
      <div className="flex items-start justify-between gap-4 rounded-xl border border-primary bg-primary-light/40 p-4">
        <div>
          <p className="font-semibold">{valor.nome}</p>
          {valor.endereco && <p className="text-sm text-texto-suave">{valor.endereco}</p>}
          {valor.tipo && <p className="text-sm text-texto-suave">{TIPOS_LOCAL[valor.tipo]}</p>}
        </div>
        <button
          type="button"
          onClick={() => aoEscolher(null)}
          className="shrink-0 text-sm font-semibold text-primary hover:underline"
        >
          Trocar
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      {erro && <p className="text-sm text-erro">{erro}</p>}
      {modo === "buscar" ? (
        <BuscaLocal aoEscolher={aoEscolher} aoCadastrarNovo={() => setModo("novo")} />
      ) : (
        <NovoLocal
          aoCriar={(local) => {
            setModo("buscar");
            aoEscolher(local);
          }}
          aoCancelar={() => setModo("buscar")}
        />
      )}
    </div>
  );
}

// ---------- buscar ----------

function BuscaLocal({
  aoEscolher,
  aoCadastrarNovo,
}: {
  aoEscolher: (local: Local) => void;
  aoCadastrarNovo: () => void;
}) {
  const [texto, setTexto] = useState("");
  const [locais, setLocais] = useState<Local[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  // Espera a pessoa parar de digitar (350 ms) antes de buscar.
  useEffect(() => {
    let ativo = true;
    const timer = setTimeout(() => {
      listarLocais({ texto: texto.trim() || undefined })
        .then((lista) => {
          if (!ativo) return;
          setLocais(lista);
          setErro("");
        })
        .catch((e) => ativo && setErro(mensagemDeErro(e)))
        .finally(() => ativo && setCarregando(false));
    }, 350);
    return () => {
      ativo = false;
      clearTimeout(timer);
    };
  }, [texto]);

  return (
    <>
      <CampoTexto
        id="busca-local"
        label="Buscar local"
        type="search"
        placeholder="Nome ou endereço (ex.: Ibirapuera)"
        value={texto}
        onChange={(e) => {
          setTexto(e.target.value);
          setCarregando(true);
        }}
      />

      <div aria-live="polite" aria-busy={carregando}>
        {carregando ? (
          <p className="text-sm text-texto-suave">Buscando locais...</p>
        ) : erro ? (
          <Alerta tipo="aviso">Não foi possível buscar os locais: {erro}</Alerta>
        ) : locais.length === 0 ? (
          <p className="text-sm text-texto-suave">
            Nenhum local encontrado{texto.trim() && ` para "${texto.trim()}"`}. Cadastre um novo abaixo.
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {locais.slice(0, MAX_RESULTADOS).map((local) => (
              <li key={local.id}>
                <button
                  type="button"
                  onClick={() => aoEscolher(local)}
                  className="w-full rounded-xl border border-borda p-3 text-left transition hover:border-primary hover:bg-primary-light/30"
                >
                  <span className="block font-medium">
                    {local.nome}
                    {local.publico === false && (
                      <span className="ml-2 rounded-full bg-fundo px-2 py-0.5 text-xs text-texto-suave">
                        Privado
                      </span>
                    )}
                  </span>
                  <span className="block text-sm text-texto-suave">
                    {[local.tipo && TIPOS_LOCAL[local.tipo], local.endereco].filter(Boolean).join(" · ")}
                    {local.quadras && local.quadras.length > 0 &&
                      ` · ${local.quadras.length} quadra${local.quadras.length > 1 ? "s" : ""}`}
                  </span>
                </button>
              </li>
            ))}
            {locais.length > MAX_RESULTADOS && (
              <li className="text-sm text-texto-suave">
                Mostrando {MAX_RESULTADOS} de {locais.length}. Digite para refinar a busca.
              </li>
            )}
          </ul>
        )}
      </div>

      <button
        type="button"
        onClick={aoCadastrarNovo}
        className="self-start text-sm font-semibold text-primary hover:underline"
      >
        + Cadastrar novo local
      </button>
    </>
  );
}

// ---------- cadastrar ----------

function NovoLocal({
  aoCriar,
  aoCancelar,
}: {
  aoCriar: (local: Local) => void;
  aoCancelar: () => void;
}) {
  const [nome, setNome] = useState("");
  const [endereco, setEndereco] = useState("");
  const [tipo, setTipo] = useState<TipoLocal>("OUTRO");
  const [publico, setPublico] = useState(true);
  const [posicao, setPosicao] = useState<Posicao | null>(null);
  const [localizando, setLocalizando] = useState(false);
  const [avisoPosicao, setAvisoPosicao] = useState("");

  const [erros, setErros] = useState<Record<string, string>>({});
  const [erroGeral, setErroGeral] = useState("");
  const [enviando, setEnviando] = useState(false);

  async function usarMinhaLocalizacao() {
    setLocalizando(true);
    setAvisoPosicao("");
    const p = await obterPosicao();
    setLocalizando(false);
    if (p) setPosicao(p);
    else setAvisoPosicao("Não conseguimos sua localização. Verifique a permissão do navegador.");
  }

  async function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral("");
    if (!nome.trim()) {
      setErros({ nome: "Informe o nome do local." });
      return;
    }
    setErros({});
    setEnviando(true);
    try {
      const local = await criarLocal({
        nome: nome.trim(),
        endereco: endereco.trim() || undefined,
        tipo,
        publico,
        // Latitude e longitude vão juntas (ou nenhuma).
        latitude: posicao?.latitude,
        longitude: posicao?.longitude,
      });
      aoCriar(local);
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 400 && erro.campos.length > 0) {
        setErros(erro.errosPorCampo());
      } else {
        setErroGeral(mensagemDeErro(erro));
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-4 rounded-xl border border-borda p-4">
      <h3 className="font-semibold">Novo local</h3>
      {erroGeral && <Alerta>{erroGeral}</Alerta>}

      <div className="grid items-start gap-4 sm:grid-cols-2">
        <CampoTexto
          id="local-nome"
          label="Nome *"
          placeholder="Ex.: Parque Ibirapuera"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          erro={erros.nome}
        />
        <CampoSelecao
          id="local-tipo"
          label="Tipo"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as TipoLocal)}
          erro={erros.tipo}
        >
          {Object.entries(TIPOS_LOCAL).map(([valor, texto]) => (
            <option key={valor} value={valor}>
              {texto}
            </option>
          ))}
        </CampoSelecao>
      </div>

      <CampoTexto
        id="local-endereco"
        label="Endereço"
        placeholder="Rua, número, bairro, cidade"
        value={endereco}
        onChange={(e) => setEndereco(e.target.value)}
        erro={erros.endereco}
      />

      <div className="flex flex-col gap-1">
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={usarMinhaLocalizacao}
            disabled={localizando}
            className="rounded-lg border border-borda px-3 py-2 text-sm font-medium hover:border-primary disabled:opacity-60"
          >
            {localizando ? "Obtendo localização..." : "📍 Usar minha localização atual"}
          </button>
          {posicao && (
            <span className="text-sm text-primary">
              ✓ Localização adicionada{" "}
              <button type="button" onClick={() => setPosicao(null)} className="text-texto-suave underline">
                remover
              </button>
            </span>
          )}
        </div>
        <p className="text-sm text-texto-suave">
          Opcional. Com a localização, o evento aparece no filtro &quot;perto de mim&quot;.
        </p>
        {avisoPosicao && <p className="text-sm text-aviso">{avisoPosicao}</p>}
      </div>

      <label className="flex items-start gap-3">
        <input
          type="checkbox"
          checked={!publico}
          onChange={(e) => setPublico(!e.target.checked)}
          className="mt-1 h-4 w-4 accent-primary"
        />
        <span>
          <span className="font-medium">Local privado</span>
          <span className="block text-sm text-texto-suave">
            Ex.: a casa de alguém. Não aparece nas buscas de outras pessoas, nem os eventos dele. Só
            entra quem receber o link do evento.
          </span>
        </span>
      </label>

      <div className="flex gap-2">
        <Botao type="submit" carregando={enviando} className="w-auto! px-6">
          Cadastrar local
        </Botao>
        <Botao variante="texto" onClick={aoCancelar} className="w-auto! px-4">
          Cancelar
        </Botao>
      </div>
    </form>
  );
}
