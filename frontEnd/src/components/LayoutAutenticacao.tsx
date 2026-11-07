// Layout das telas de entrada (Login e Criar Conta).
// - Computador (lg+): painel verde da marca à esquerda, formulário à direita.
// - Celular/tablet: faixa verde compacta no topo e o formulário embaixo.

import { Logo } from "@/components/Logo";

const DESTAQUES = [
  "Encontre peladas, corridas e treinos perto de você",
  "Crie seus eventos e convide a galera",
  "Filtre por esporte, local, data e distância",
];

export function LayoutAutenticacao({
  children,
  larguraFormulario = "max-w-md",
}: {
  children: React.ReactNode;
  /** Classe de largura máxima do formulário (o cadastro é mais largo). */
  larguraFormulario?: string;
}) {
  return (
    <div className="grid flex-1 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
      {/* Painel da marca (fica parado enquanto o formulário rola) */}
      <aside className="bg-gradient-to-br from-primary to-primary-dark px-6 py-8 text-white lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:justify-center lg:px-12">
        <Logo claro comSlogan />
        <ul className="mt-10 hidden flex-col gap-4 text-lg text-white/90 lg:flex">
          {DESTAQUES.map((texto) => (
            <li key={texto} className="flex items-start gap-3">
              <span aria-hidden className="mt-1 text-secondary">●</span>
              {texto}
            </li>
          ))}
        </ul>
      </aside>

      {/* Formulário */}
      <main className="flex items-start justify-center px-4 py-10 sm:px-6 lg:items-center lg:py-12">
        <div className={`w-full ${larguraFormulario} rounded-2xl bg-superficie p-6 shadow-sm sm:p-8`}>
          {children}
        </div>
      </main>
    </div>
  );
}
