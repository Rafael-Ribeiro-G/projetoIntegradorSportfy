"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoTexto } from "@/components/CampoTexto";
import { LayoutAutenticacao } from "@/components/LayoutAutenticacao";
import { useAuth } from "@/contexts/AuthContext";
import { ApiError } from "@/services/api";

type Erros = Partial<Record<"email" | "senha", string>>;

export default function LoginPage() {
  const router = useRouter();
  const { login, token, carregando } = useAuth();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erros, setErros] = useState<Erros>({});
  const [erroGeral, setErroGeral] = useState("");
  const [enviando, setEnviando] = useState(false);

  // Quem já está logado não precisa ver o login.
  useEffect(() => {
    if (!carregando && token) router.replace("/");
  }, [carregando, token, router]);

  function validar(): Erros {
    const novos: Erros = {};
    if (!email.trim()) novos.email = "Informe seu e-mail.";
    if (!senha) novos.senha = "Informe sua senha.";
    return novos;
  }

  async function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral("");

    const novos = validar();
    setErros(novos);
    if (Object.keys(novos).length > 0) return;

    setEnviando(true);
    try {
      await login({ email: email.trim(), senha });
      router.replace("/");
    } catch (erro) {
      if (!(erro instanceof ApiError)) {
        setErroGeral("Algo deu errado. Tente de novo.");
      } else if (erro.status === 400) {
        setErros(erro.errosPorCampo());
        if (erro.campos.length === 0) setErroGeral(erro.mensagem);
      } else if (erro.status === 401) {
        setErroGeral("E-mail ou senha incorretos.");
      } else if (erro.status === 429) {
        setErroGeral("Muitas tentativas, aguarde 15 minutos.");
      } else {
        setErroGeral(erro.mensagem);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <LayoutAutenticacao>
      <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-4">
        <div>
          <h1 className="text-2xl font-bold">Entrar</h1>
          <p className="text-sm text-texto-suave">Que bom te ver de novo!</p>
        </div>

        {erroGeral && <Alerta>{erroGeral}</Alerta>}

        <CampoTexto
          id="email"
          label="E-mail"
          type="email"
          autoComplete="email"
          inputMode="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          erro={erros.email}
        />
        <CampoTexto
          id="senha"
          label="Senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          erro={erros.senha}
        />

        <Botao type="submit" carregando={enviando}>
          Entrar
        </Botao>
      </form>

      <p className="mt-6 text-center text-sm text-texto-suave">
        Ainda não tem conta?{" "}
        <Link href="/cadastro" className="font-semibold text-primary hover:underline">
          Criar conta
        </Link>
      </p>
    </LayoutAutenticacao>
  );
}
