"use client";

// Conta / Perfil: meus dados + meus eventos + trocar senha + LGPD + sair.

import { useEffect } from "react";
import Link from "next/link";
import { Avatar } from "@/components/Avatar";
import { Botao } from "@/components/Botao";
import { MeusEventos } from "@/components/perfil/MeusEventos";
import { PrivacidadeLgpd } from "@/components/perfil/PrivacidadeLgpd";
import { TrocarSenha } from "@/components/perfil/TrocarSenha";
import { TituloPagina } from "@/components/TituloPagina";
import { useAuth } from "@/contexts/AuthContext";
import { textoNivel } from "@/lib/rotulos";
import { buscarMeuUsuario } from "@/services/usuarios";

export default function PerfilPage() {
  const { usuario, logout, atualizarUsuario } = useAuth();

  // O usuário salvo no navegador pode estar desatualizado: busca a versão atual da API.
  useEffect(() => {
    let ativo = true;
    buscarMeuUsuario()
      .then((u) => ativo && atualizarUsuario(u))
      .catch(() => {}); // sem rede, seguimos com o que está salvo
    return () => {
      ativo = false;
    };
  }, [atualizarUsuario]);

  if (!usuario) return null; // o layout (logado) já garante que existe

  return (
    <main>
      <TituloPagina titulo="Meu perfil" />

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Cartão com meus dados */}
        <aside className="flex flex-col gap-5 self-start rounded-2xl bg-superficie p-6 shadow-sm ring-1 ring-borda lg:sticky lg:top-24">
          <div className="flex flex-col items-center gap-3 text-center">
            <Avatar nome={usuario.nome} fotoUrl={usuario.fotoUrl} tamanho="grande" />
            <div>
              <p className="text-xl font-bold">{usuario.nome}</p>
              {usuario.nivelHabilidade !== undefined && (
                <p className="text-sm text-texto-suave">Nível {textoNivel(usuario.nivelHabilidade)}</p>
              )}
            </div>
          </div>

          {usuario.bio && <p className="whitespace-pre-line text-center">{usuario.bio}</p>}

          <dl className="flex flex-col gap-2 border-t border-borda pt-4 text-sm">
            <div>
              <dt className="text-texto-suave">E-mail</dt>
              <dd className="break-all font-medium">{usuario.email}</dd>
            </div>
            <div>
              <dt className="text-texto-suave">Telefone</dt>
              <dd className="font-medium">{usuario.telefone || "Não informado"}</dd>
            </div>
            <p className="text-xs text-texto-suave">E-mail e telefone não aparecem para outras pessoas.</p>
          </dl>

          {usuario.esportesFavoritos && usuario.esportesFavoritos.length > 0 && (
            <div className="flex flex-col gap-2">
              <h2 className="text-sm text-texto-suave">Esportes favoritos</h2>
              <ul className="flex flex-wrap gap-2">
                {usuario.esportesFavoritos.map((esp) => (
                  <li key={esp.id} className="rounded-full bg-primary-light px-3 py-1 text-sm font-medium text-primary-dark">
                    {esp.nome}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex flex-col gap-2">
            <Link
              href="/perfil/editar"
              className="rounded-xl bg-primary px-4 py-3 text-center font-semibold text-white transition hover:bg-primary-dark"
            >
              Editar perfil
            </Link>
            <Botao variante="secundario" onClick={logout}>
              Sair
            </Botao>
          </div>
        </aside>

        {/* Seções */}
        <div className="flex flex-col gap-6 lg:col-span-2">
          <MeusEventos />
          <TrocarSenha />
          <PrivacidadeLgpd />
        </div>
      </div>
    </main>
  );
}
