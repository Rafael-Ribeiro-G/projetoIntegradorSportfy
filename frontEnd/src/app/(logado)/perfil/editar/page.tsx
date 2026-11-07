"use client";

// Editar perfil. ATENÇÃO (regra da API): no PUT /api/usuarios/me, campo null é APAGADO.
// Por isso o formulário começa com os dados atuais e o envio manda sempre TODOS os campos.

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoFoto, type ValorFoto } from "@/components/CampoFoto";
import { CampoAreaTexto, CampoSelecao, CampoTexto } from "@/components/CampoTexto";
import { SeletorEsportes } from "@/components/SeletorEsportes";
import { TituloPagina } from "@/components/TituloPagina";
import { useAuth } from "@/contexts/AuthContext";
import { NIVEIS } from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import { enviarImagem } from "@/services/uploads";
import { atualizarMeuUsuario, buscarMeuUsuario } from "@/services/usuarios";
import type { NivelHabilidade, Usuario } from "@/types/usuario";

type Erros = Record<string, string>;

export default function EditarPerfilPage() {
  const router = useRouter();
  const { atualizarUsuario } = useAuth();

  const [original, setOriginal] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erroCarregar, setErroCarregar] = useState("");
  const [versao, setVersao] = useState(0);

  // Campos
  const [nome, setNome] = useState("");
  const [telefone, setTelefone] = useState("");
  const [nivel, setNivel] = useState<NivelHabilidade | "">("");
  const [bio, setBio] = useState("");
  const [favoritos, setFavoritos] = useState<number[]>([]);
  const [foto, setFoto] = useState<ValorFoto>({ arquivo: null, removida: false });

  const [erros, setErros] = useState<Erros>({});
  const [erroGeral, setErroGeral] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Busca os dados atuais e preenche o formulário.
  useEffect(() => {
    let ativo = true;
    buscarMeuUsuario()
      .then((u) => {
        if (!ativo) return;
        setOriginal(u);
        setNome(u.nome);
        setTelefone(u.telefone ?? "");
        setNivel(u.nivelHabilidade ?? "");
        setBio(u.bio ?? "");
        setFavoritos(u.esportesFavoritos?.map((e) => e.id) ?? []);
        setErroCarregar("");
      })
      .catch((e) => ativo && setErroCarregar(mensagemDeErro(e)))
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [versao]);

  async function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral("");
    if (!original) return;
    if (!nome.trim()) {
      setErros({ nome: "Informe seu nome." });
      setErroGeral("Confira os campos destacados.");
      return;
    }
    setErros({});
    setEnviando(true);
    try {
      // 1) Foto: nova → envia e usa a url; removida → null; senão mantém a atual.
      let fotoUrl: string | null = foto.removida ? null : (original.fotoUrl ?? null);
      if (foto.arquivo) {
        try {
          fotoUrl = (await enviarImagem(foto.arquivo)).url;
        } catch (erro) {
          setErros({ fotoUrl: mensagemDeErro(erro) });
          setErroGeral("Não foi possível enviar a foto.");
          return;
        }
      }

      // 2) PUT com TODOS os campos (vazio vira null = apagar).
      const atualizado = await atualizarMeuUsuario({
        nome: nome.trim(),
        telefone: telefone.trim() || null,
        nivelHabilidade: nivel || null,
        bio: bio.trim() || null,
        fotoUrl,
        esportesFavoritosIds: favoritos, // a lista substitui a atual
      });
      atualizarUsuario(atualizado);
      router.push("/perfil");
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 400 && erro.campos.length > 0) {
        setErros(erro.errosPorCampo());
        setErroGeral("Confira os campos destacados.");
      } else {
        setErroGeral(mensagemDeErro(erro));
      }
    } finally {
      setEnviando(false);
    }
  }

  if (carregando) {
    return (
      <main aria-busy className="mx-auto w-full max-w-2xl animate-pulse">
        <div className="mb-6 h-8 w-1/2 rounded bg-borda" />
        <div className="h-96 rounded-2xl bg-borda" />
      </main>
    );
  }

  if (erroCarregar || !original) {
    return (
      <main className="mx-auto flex max-w-xl flex-col items-center gap-4 py-10 text-center">
        <h1 className="text-2xl font-bold">Não foi possível abrir seu perfil</h1>
        <p className="text-texto-suave">{erroCarregar}</p>
        <Botao
          className="w-auto! px-6"
          onClick={() => {
            setCarregando(true);
            setVersao((v) => v + 1);
          }}
        >
          Tentar de novo
        </Botao>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-2xl">
      <Link href="/perfil" className="mb-4 inline-block text-sm font-semibold text-primary hover:underline">
        ← Voltar ao perfil
      </Link>
      <TituloPagina titulo="Editar perfil" />

      <form
        onSubmit={aoEnviar}
        noValidate
        className="flex flex-col gap-5 rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:p-6"
      >
        {erroGeral && <Alerta>{erroGeral}</Alerta>}

        <CampoFoto
          id="foto"
          nome={nome}
          fotoAtual={original.fotoUrl}
          aoMudar={setFoto}
          erro={erros.fotoUrl}
        />

        <div className="grid items-start gap-4 sm:grid-cols-2">
          <CampoTexto
            id="nome"
            label="Nome *"
            autoComplete="name"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            erro={erros.nome}
          />
          <CampoTexto
            id="email"
            label="E-mail"
            value={original.email}
            disabled
            dica="O e-mail não pode ser alterado."
          />
          <CampoTexto
            id="telefone"
            label="Telefone"
            type="tel"
            inputMode="tel"
            autoComplete="tel"
            placeholder="(11) 98888-7777"
            value={telefone}
            onChange={(e) => setTelefone(e.target.value)}
            erro={erros.telefone}
          />
          <CampoSelecao
            id="nivelHabilidade"
            label="Nível de habilidade"
            value={nivel}
            onChange={(e) => setNivel(e.target.value ? (Number(e.target.value) as NivelHabilidade) : "")}
            erro={erros.nivelHabilidade}
          >
            <option value="">Não informar</option>
            {NIVEIS.map((n) => (
              <option key={n.valor} value={n.valor}>
                {n.texto}
              </option>
            ))}
          </CampoSelecao>
        </div>

        <CampoAreaTexto
          id="bio"
          label="Bio"
          placeholder="Ex.: Corro aos domingos"
          value={bio}
          onChange={(e) => setBio(e.target.value)}
          erro={erros.bio}
        />

        <SeletorEsportes selecionados={favoritos} aoMudar={setFavoritos} />

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Link
            href="/perfil"
            className="rounded-xl px-6 py-3 text-center font-semibold text-texto-suave hover:bg-fundo"
          >
            Cancelar
          </Link>
          <Botao type="submit" carregando={enviando} className="sm:w-auto! sm:px-8">
            Salvar alterações
          </Botao>
        </div>
      </form>
    </main>
  );
}
