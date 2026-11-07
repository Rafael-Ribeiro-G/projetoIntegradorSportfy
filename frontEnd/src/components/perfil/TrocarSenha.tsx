"use client";

// Troca de senha. A API devolve um token NOVO (o antigo para de valer), então salvamos a sessão de novo.

import { useState, type FormEvent } from "react";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoTexto } from "@/components/CampoTexto";
import { useAuth } from "@/contexts/AuthContext";
import { MENSAGEM_SENHA, SENHA_VALIDA } from "@/lib/validacao";
import { ApiError, mensagemDeErro } from "@/services/api";
import { trocarSenha } from "@/services/usuarios";

type Erros = Record<string, string>;

export function TrocarSenha() {
  const { usuario, salvarSessao } = useAuth();
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [aviso, setAviso] = useState<{ tipo: "sucesso" | "erro"; texto: string } | null>(null);
  const [enviando, setEnviando] = useState(false);

  function validar(): Erros {
    const novos: Erros = {};
    if (!senhaAtual) novos.senhaAtual = "Informe sua senha atual.";
    if (!SENHA_VALIDA.test(novaSenha)) novos.novaSenha = MENSAGEM_SENHA;
    else if (novaSenha === senhaAtual) novos.novaSenha = "A nova senha precisa ser diferente da atual.";
    if (confirmar !== novaSenha) novos.confirmar = "As senhas não são iguais.";
    return novos;
  }

  async function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setAviso(null);
    const novos = validar();
    setErros(novos);
    if (Object.keys(novos).length > 0 || !usuario) return;

    setEnviando(true);
    try {
      const resposta = await trocarSenha({ senhaAtual, novaSenha });
      // Substitui o token antigo pelo novo (mantendo o usuário, se a resposta não trouxer).
      salvarSessao({
        token: resposta.token,
        tipo: resposta.tipo ?? "Bearer",
        expiraEmSegundos: resposta.expiraEmSegundos ?? 24 * 60 * 60,
        usuario: resposta.usuario ?? usuario,
      });
      setSenhaAtual("");
      setNovaSenha("");
      setConfirmar("");
      setAviso({ tipo: "sucesso", texto: "Senha alterada com sucesso." });
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 400 && erro.campos.length > 0) {
        setErros(erro.errosPorCampo());
      } else if (erro instanceof ApiError && (erro.status === 401 || erro.status === 403)) {
        setErros({ senhaAtual: "Senha atual incorreta." });
      } else {
        setAviso({ tipo: "erro", texto: mensagemDeErro(erro) });
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section aria-labelledby="titulo-senha" className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:p-6">
      <h2 id="titulo-senha" className="mb-4 text-lg font-semibold">
        Trocar senha
      </h2>
      <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-4">
        {aviso && <Alerta tipo={aviso.tipo}>{aviso.texto}</Alerta>}
        <CampoTexto
          id="senhaAtual"
          label="Senha atual"
          type="password"
          autoComplete="current-password"
          value={senhaAtual}
          onChange={(e) => setSenhaAtual(e.target.value)}
          erro={erros.senhaAtual}
        />
        <div className="grid items-start gap-4 sm:grid-cols-2">
          <CampoTexto
            id="novaSenha"
            label="Nova senha"
            type="password"
            autoComplete="new-password"
            value={novaSenha}
            onChange={(e) => setNovaSenha(e.target.value)}
            erro={erros.novaSenha}
            dica="8 ou mais caracteres, com letra e número."
          />
          <CampoTexto
            id="confirmarNovaSenha"
            label="Confirmar nova senha"
            type="password"
            autoComplete="new-password"
            value={confirmar}
            onChange={(e) => setConfirmar(e.target.value)}
            erro={erros.confirmar}
          />
        </div>
        <Botao type="submit" carregando={enviando} className="sm:w-auto! sm:self-start sm:px-8">
          Alterar senha
        </Botao>
      </form>
    </section>
  );
}
