import type { Metadata, Viewport } from "next";
import { Geist } from "next/font/google";
import { AuthProvider } from "@/contexts/AuthContext";
import { SCRIPT_TEMA_INICIAL } from "@/lib/temaScript";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "sportfy.",
  description: "Pratique, Conecte e Divirta-se",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // Cor da barra do navegador no celular, conforme o tema do sistema.
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#16a34a" },
    { media: "(prefers-color-scheme: dark)", color: "#0b1320" },
  ],
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // suppressHydrationWarning: o script abaixo coloca data-tema no <html> antes do React,
    // então o atributo é diferente do que veio do servidor (isso é esperado).
    <html lang="pt-BR" className={`${geistSans.variable} antialiased`} suppressHydrationWarning>
      <head>
        {/*
          Script direto no <head>: o navegador executa ao ler o HTML, ANTES de desenhar a página,
          então quem usa o modo escuro não vê um "flash" branco. (O next/script com beforeInteractive
          só roda depois que o JavaScript do Next carrega, tarde demais para isso.)
          Em modo dev o React avisa "Encountered a script tag..."; é só um aviso de desenvolvimento.
        */}
        <script dangerouslySetInnerHTML={{ __html: SCRIPT_TEMA_INICIAL }} />
      </head>
      {/* Site responsivo: cada layout/tela decide a largura do próprio conteúdo. */}
      <body className="flex min-h-dvh flex-col font-sans">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
