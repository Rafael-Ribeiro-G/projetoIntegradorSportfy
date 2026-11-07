"use client";

// Layout de todas as telas que exigem login (o grupo "(logado)" não aparece na URL).
// 1) Espera o AuthContext ler o localStorage.
// 2) Sem token → manda para /login.
// 3) Com token → cabeçalho de navegação + conteúdo centralizado (até max-w-6xl) + SportIA.

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { BarraNavegacao } from "@/components/BarraNavegacao";
import { ChatSportIA } from "@/components/ChatSportIA";
import { useAuth } from "@/contexts/AuthContext";

export default function LogadoLayout({ children }: LayoutProps<"/">) {
  const router = useRouter();
  const { token, carregando } = useAuth();

  useEffect(() => {
    if (!carregando && !token) router.replace("/login");
  }, [carregando, token, router]);

  // Enquanto não sabemos se está logado (ou já está indo para o login), não mostramos a tela.
  if (carregando || !token) {
    return (
      <p role="status" className="m-auto text-texto-suave">
        Carregando...
      </p>
    );
  }

  return (
    <>
      <BarraNavegacao />
      {/* pb-24: espaço para o botão flutuante do SportIA não cobrir o fim da página. */}
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col px-4 pb-24 pt-6 md:pt-8">
        {children}
      </div>
      <ChatSportIA />
    </>
  );
}
