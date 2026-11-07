"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Alerta } from "@/components/Alerta";
import { Botao } from "@/components/Botao";
import { CampoFoto } from "@/components/CampoFoto";
import { CampoAreaTexto, CampoSelecao, CampoTexto } from "@/components/CampoTexto";
import { LayoutAutenticacao } from "@/components/LayoutAutenticacao";
import { SeletorEsportes } from "@/components/SeletorEsportes";
import { useAuth } from "@/contexts/AuthContext";
import { NIVEIS } from "@/lib/rotulos";
import { EMAIL_VALIDO, MENSAGEM_SENHA, SENHA_VALIDA } from "@/lib/validacao";
import { ApiError, mensagemDeErro } from "@/services/api";
import { enviarImagem } from "@/services/uploads";
import { atualizarMeuUsuario } from "@/services/usuarios";
import type { NivelHabilidade } from "@/types/usuario";

type Erros = Record<string, string>;

export default function CadastroPage() {
  const router = useRouter();
  const { registrar, atualizarUsuario, token, carregando } = useAuth();

  // Campos do formulário
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [confirmarSenha, setConfirmarSenha] = useState("");
  const [telefone, setTelefone] = useState("");
  const [nivel, setNivel] = useState<NivelHabilidade | "">("");
  const [bio, setBio] = useState("");
  const [favoritos, setFavoritos] = useState<number[]>([]);
  const [foto, setFoto] = useState<File | null>(null);

  const [erros, setErros] = useState<Erros>({});
  const [erroGeral, setErroGeral] = useState("");
  const [enviando, setEnviando] = useState(false);
  // Conta criada, mas a foto falhou: mostramos o aviso em vez de redirecionar direto.
  const [avisoFoto, setAvisoFoto] = useState("");

  // Quem já está logado vai para a Home (exceto logo após cadastrar, com aviso pendente).
  useEffect(() => {
    if (!carregando && token && !enviando && !avisoFoto) router.replace("/");
  }, [carregando, token, enviando, avisoFoto, router]);

  function validar(): Erros {
    const novos: Erros = {};
    if (!nome.trim()) novos.nome = "Informe seu nome.";
    if (!email.trim()) novos.email = "Informe seu e-mail.";
    else if (!EMAIL_VALIDO.test(email.trim())) novos.email = "E-mail inválido.";
    if (!SENHA_VALIDA.test(senha)) novos.senha = MENSAGEM_SENHA;
    if (confirmarSenha !== senha) novos.confirmarSenha = "As senhas não são iguais.";
    return novos;
  }

  async function aoEnviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErroGeral("");

    const novos = validar();
    setErros(novos);
    if (Object.keys(novos).length > 0) {
      setErroGeral("Confira os campos destacados.");
      return;
    }

    setEnviando(true);
    try {
      // 1) Cria a conta. A resposta já traz o token, então a pessoa fica logada.
      const usuario = await registrar({
        nome: nome.trim(),
        email: email.trim(),
        senha,
        telefone: telefone.trim() || undefined,
        nivelHabilidade: nivel || undefined,
        bio: bio.trim() || undefined,
        esportesFavoritosIds: favoritos.length > 0 ? favoritos : undefined,
      });

      // 2) Foto: o upload exige login, por isso só acontece agora.
      if (foto) {
        try {
          const { url } = await enviarImagem(foto);
          // No PUT, campo null é apagado: mandamos tudo o que a pessoa acabou de preencher.
          // Sem esportesFavoritosIds, os favoritos ficam como estão.
          const atualizado = await atualizarMeuUsuario({
            nome: usuario.nome,
            telefone: usuario.telefone ?? null,
            nivelHabilidade: usuario.nivelHabilidade ?? null,
            bio: usuario.bio ?? null,
            fotoUrl: url,
          });
          atualizarUsuario(atualizado);
        } catch (erroFoto) {
          setAvisoFoto(
            `Sua conta foi criada, mas a foto não foi enviada (${mensagemDeErro(erroFoto)}). Você pode adicioná-la depois no Perfil.`,
          );
          return;
        }
      }

      router.replace("/");
    } catch (erro) {
      if (erro instanceof ApiError && erro.status === 400 && erro.campos.length > 0) {
        setErros(erro.errosPorCampo());
        setErroGeral("Confira os campos destacados.");
      } else {
        // 409 = e-mail já cadastrado, entre outros: a API manda a mensagem pronta.
        setErroGeral(mensagemDeErro(erro));
      }
    } finally {
      setEnviando(false);
    }
  }

  // Tela de aviso depois do cadastro (foto falhou)
  if (avisoFoto) {
    return (
      <LayoutAutenticacao>
        <div className="flex flex-col gap-6">
          <h1 className="text-2xl font-bold">Conta criada!</h1>
          <Alerta tipo="aviso">{avisoFoto}</Alerta>
          <Botao onClick={() => router.replace("/")}>Continuar</Botao>
        </div>
      </LayoutAutenticacao>
    );
  }

  return (
    <LayoutAutenticacao larguraFormulario="max-w-2xl">
      <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-4">
        <div>
          <h1 className="text-2xl font-bold">Criar conta</h1>
          <p className="text-sm text-texto-suave">Leva menos de um minuto. Campos com * são obrigatórios.</p>
        </div>

        {erroGeral && <Alerta>{erroGeral}</Alerta>}

        {/* No computador, dois campos por linha; no celular, um embaixo do outro. */}
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
            label="E-mail *"
            type="email"
            inputMode="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            erro={erros.email}
          />
          <CampoTexto
            id="senha"
            label="Senha *"
            type="password"
            autoComplete="new-password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            erro={erros.senha}
            dica="8 ou mais caracteres, com letra e número."
          />
          <CampoTexto
            id="confirmarSenha"
            label="Confirmar senha *"
            type="password"
            autoComplete="new-password"
            value={confirmarSenha}
            onChange={(e) => setConfirmarSenha(e.target.value)}
            erro={erros.confirmarSenha}
          />
        </div>

        <h2 className="mt-2 text-lg font-semibold">
          Sobre você <span className="text-sm font-normal text-texto-suave">(opcional)</span>
        </h2>

        <div className="grid items-start gap-4 sm:grid-cols-2">
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

        <CampoFoto id="foto" nome={nome} aoMudar={({ arquivo }) => setFoto(arquivo)} erro={erros.fotoUrl} />

        <SeletorEsportes selecionados={favoritos} aoMudar={setFavoritos} />

        {/* LGPD */}
        <Alerta tipo="info">
          <strong>Privacidade (LGPD):</strong> usamos seus dados apenas para o funcionamento do
          Sportfy (perfil, eventos e participações). Seu e-mail e telefone não aparecem para outras
          pessoas. A qualquer momento você pode baixar seus dados ou excluir sua conta no Perfil.
        </Alerta>

        <Botao type="submit" carregando={enviando}>
          Criar conta
        </Botao>
      </form>

      <p className="mt-6 text-center text-sm text-texto-suave">
        Já tem conta?{" "}
        <Link href="/login" className="font-semibold text-primary hover:underline">
          Entrar
        </Link>
      </p>
    </LayoutAutenticacao>
  );
}
