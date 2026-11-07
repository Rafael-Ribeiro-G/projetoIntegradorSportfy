"use client";

// LGPD: baixar todos os meus dados (arquivo JSON) e excluir a conta (senha + confirmação dupla).

import { useState, type FormEvent } from "react";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoTexto } from "@/components/CampoTexto";
import { useAuth } from "@/contexts/AuthContext";
import { agoraDatetimeLocal } from "@/lib/datas";
import { ApiError, mensagemDeErro } from "@/services/api";
import { excluirMinhaConta, exportarMeusDados } from "@/services/usuarios";

/** Cria um arquivo no navegador e dispara o download (sem passar por servidor). */
function baixarJson(dados: unknown, nomeArquivo: string) {
  const blob = new Blob([JSON.stringify(dados, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = nomeArquivo;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function PrivacidadeLgpd() {
  const { logout } = useAuth();

  // Baixar dados
  const [baixando, setBaixando] = useState(false);
  const [avisoDados, setAvisoDados] = useState<{ tipo: "sucesso" | "erro"; texto: string } | null>(null);

  // Excluir conta
  const [excluindoAberto, setExcluindoAberto] = useState(false);
  const [senha, setSenha] = useState("");
  const [entendo, setEntendo] = useState(false);
  const [erroSenha, setErroSenha] = useState("");
  const [erroExclusao, setErroExclusao] = useState("");
  const [excluindo, setExcluindo] = useState(false);

  async function baixarMeusDados() {
    setBaixando(true);
    setAvisoDados(null);
    try {
      const dados = await exportarMeusDados();
      const hoje = agoraDatetimeLocal().slice(0, 10); // "2026-11-07"
      baixarJson(dados, `sportfy-meus-dados-${hoje}.json`);
      setAvisoDados({ tipo: "sucesso", texto: "Pronto! O arquivo com seus dados foi baixado." });
    } catch (erro) {
      setAvisoDados({ tipo: "erro", texto: mensagemDeErro(erro) });
    } finally {
      setBaixando(false);
    }
  }

  function fecharExclusao() {
    setExcluindoAberto(false);
    setSenha("");
    setEntendo(false);
    setErroSenha("");
    setErroExclusao("");
  }

  async function excluirConta(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroSenha("");
    setErroExclusao("");
    if (!senha) {
      setErroSenha("Informe sua senha para confirmar.");
      return;
    }
    // Segunda confirmação (a primeira é a caixa "Entendo...").
    const ok = window.confirm(
      "Última confirmação: excluir sua conta do Sportfy agora? Isso apaga seus dados e os eventos que você criou, e não pode ser desfeito.",
    );
    if (!ok) return;

    setExcluindo(true);
    try {
      await excluirMinhaConta({ senha });
      logout(); // limpa a sessão e vai para /login
    } catch (erro) {
      if (erro instanceof ApiError && [400, 401, 403].includes(erro.status)) {
        setErroSenha("Senha incorreta.");
      } else {
        setErroExclusao(mensagemDeErro(erro));
      }
      setExcluindo(false);
    }
  }

  return (
    <section aria-labelledby="titulo-lgpd" className="rounded-2xl bg-superficie p-5 shadow-sm ring-1 ring-borda sm:p-6">
      <h2 id="titulo-lgpd" className="mb-1 text-lg font-semibold">
        Privacidade (LGPD)
      </h2>
      <p className="mb-5 text-sm text-texto-suave">
        Seus dados são seus. Você pode baixar uma cópia de tudo o que o Sportfy guarda sobre você ou
        excluir sua conta a qualquer momento.
      </p>

      {/* Baixar meus dados */}
      <div className="flex flex-col gap-3 border-b border-borda pb-5">
        <div>
          <h3 className="font-medium">Baixar meus dados</h3>
          <p className="text-sm text-texto-suave">Um arquivo .json com seu perfil, eventos e participações.</p>
        </div>
        {avisoDados && <Alerta tipo={avisoDados.tipo}>{avisoDados.texto}</Alerta>}
        <Botao variante="secundario" carregando={baixando} onClick={baixarMeusDados} className="sm:w-auto! sm:self-start sm:px-6">
          Baixar meus dados
        </Botao>
      </div>

      {/* Excluir conta */}
      <div className="flex flex-col gap-3 pt-5">
        <div>
          <h3 className="font-medium text-erro">Excluir conta</h3>
          <p className="text-sm text-texto-suave">
            Apaga sua conta e os eventos que você criou. Essa ação não pode ser desfeita.
          </p>
        </div>

        {!excluindoAberto ? (
          <Botao variante="perigo" onClick={() => setExcluindoAberto(true)} className="sm:w-auto! sm:self-start sm:px-6">
            Excluir minha conta
          </Botao>
        ) : (
          <form onSubmit={excluirConta} noValidate className="flex flex-col gap-4 rounded-xl border border-erro/40 bg-erro/5 p-4">
            {erroExclusao && <Alerta>{erroExclusao}</Alerta>}
            <CampoTexto
              id="senhaExclusao"
              label="Confirme sua senha"
              type="password"
              autoComplete="current-password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              erro={erroSenha}
            />
            <label className="flex items-start gap-3 text-sm">
              <input
                type="checkbox"
                checked={entendo}
                onChange={(e) => setEntendo(e.target.checked)}
                className="mt-0.5 h-4 w-4 accent-erro"
              />
              Entendo que minha conta e os eventos que criei serão apagados para sempre.
            </label>
            <div className="flex flex-wrap gap-2">
              <Botao
                type="submit"
                variante="perigo"
                carregando={excluindo}
                disabled={!entendo}
                className="w-auto! px-6"
              >
                Excluir definitivamente
              </Botao>
              <Botao variante="texto" onClick={fecharExclusao} className="w-auto! px-4">
                Cancelar
              </Botao>
            </div>
          </form>
        )}
      </div>
    </section>
  );
}
