"use client";

// Perfil público de outra pessoa (sem e-mail e telefone). Abre ao clicar num participante.

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Avatar } from "@/components/Avatar";
import { Botao } from "@/components/Botao";
import { textoNivel } from "@/lib/rotulos";
import { ApiError, mensagemDeErro } from "@/services/api";
import { buscarUsuarioPublico } from "@/services/usuarios";
import type { UsuarioPublico } from "@/types/usuario";

export default function PerfilPublicoPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();

  const [pessoa, setPessoa] = useState<UsuarioPublico | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<{ status: number; mensagem: string } | null>(null);

  // Busca no efeito e aplica no .then(); "ativo" ignora a resposta se a pessoa já saiu da tela.
  const [tentativa, setTentativa] = useState(0);
  useEffect(() => {
    let ativo = true;
    buscarUsuarioPublico(id)
      .then((dados) => {
        if (!ativo) return;
        setPessoa(dados);
        setErro(null);
      })
      .catch((e) => {
        if (!ativo) return;
        setErro({ status: e instanceof ApiError ? e.status : 0, mensagem: mensagemDeErro(e) });
      })
      .finally(() => ativo && setCarregando(false));
    return () => {
      ativo = false;
    };
  }, [id, tentativa]);

  function tentarDeNovo() {
    setCarregando(true);
    setTentativa((t) => t + 1);
  }

  function voltar() {
    if (window.history.length > 1) router.back();
    else router.push("/eventos");
  }

  if (carregando) {
    return (
      <main aria-busy className="mx-auto w-full max-w-2xl animate-pulse">
        <div className="h-56 rounded-2xl bg-borda" />
      </main>
    );
  }

  if (erro || !pessoa) {
    const naoExiste = erro?.status === 404 || erro?.status === 400;
    return (
      <main className="mx-auto flex max-w-xl flex-col items-center gap-4 py-10 text-center">
        <h1 className="text-2xl font-bold">
          {naoExiste ? "Pessoa não encontrada" : "Não foi possível abrir o perfil"}
        </h1>
        {!naoExiste && <p className="text-texto-suave">{erro?.mensagem}</p>}
        <div className="flex gap-2">
          {!naoExiste && (
            <Botao className="w-auto! px-6" onClick={tentarDeNovo}>
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

  return (
    <main className="mx-auto w-full max-w-2xl">
      <button type="button" onClick={voltar} className="mb-4 text-sm font-semibold text-primary hover:underline">
        ← Voltar
      </button>

      <section className="flex flex-col items-center gap-4 rounded-2xl bg-superficie p-8 text-center shadow-sm ring-1 ring-borda">
        <Avatar nome={pessoa.nome} fotoUrl={pessoa.fotoUrl} tamanho="grande" />
        <div>
          <h1 className="text-2xl font-bold">{pessoa.nome}</h1>
          {pessoa.nivelHabilidade !== undefined && (
            <p className="text-texto-suave">Nível {textoNivel(pessoa.nivelHabilidade)}</p>
          )}
        </div>

        {pessoa.bio && <p className="max-w-md whitespace-pre-line">{pessoa.bio}</p>}

        {pessoa.esportesFavoritos && pessoa.esportesFavoritos.length > 0 && (
          <div className="flex flex-col items-center gap-2">
            <h2 className="text-sm font-medium text-texto-suave">Esportes favoritos</h2>
            <ul className="flex flex-wrap justify-center gap-2">
              {pessoa.esportesFavoritos.map((esp) => (
                <li key={esp.id} className="rounded-full bg-primary-light px-3 py-1 text-sm font-medium text-primary-forte">
                  {esp.nome}
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </main>
  );
}
