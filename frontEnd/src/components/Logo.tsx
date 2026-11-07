import Image, { type StaticImageData } from "next/image";
import logoBranca from "@/assets/logo/sportfy-branca.png";
import logoEscura from "@/assets/logo/sportfy-escura.png";
import logoLima from "@/assets/logo/sportfy-lima.png";
import logoVerde from "@/assets/logo/sportfy-verde.png";

/**
 * Logo oficial do Sportfy (imagem sem fundo). Escolha a cor pelo fundo onde ela fica:
 * - "escura": fundos claros (cabeçalho branco)
 * - "branca": fundos verdes ou escuros
 * - "lima": a cor original da marca, para fundos escuros (azul-marinho)
 * - "verde": o verde do tema do site
 */
export type CorLogo = "escura" | "branca" | "lima" | "verde";

const IMAGENS: Record<CorLogo, StaticImageData> = {
  escura: logoEscura,
  branca: logoBranca,
  lima: logoLima,
  verde: logoVerde,
};

const ALTURAS = {
  pequeno: "h-7", // cabeçalho
  grande: "h-12 sm:h-14", // telas de entrada
};

export function Logo({
  cor = "escura",
  corEscuro,
  tamanho = "grande",
  comSlogan = false,
  prioridade = false,
}: {
  cor?: CorLogo;
  /** Cor no modo escuro (se for diferente). Ex.: cor="escura" corEscuro="lima". */
  corEscuro?: CorLogo;
  tamanho?: keyof typeof ALTURAS;
  comSlogan?: boolean;
  /** true para o logo que aparece logo ao abrir a página (carrega antes). */
  prioridade?: boolean;
}) {
  const sloganClaro = cor === "branca" || cor === "lima";
  // A largura acompanha a altura, mantendo a proporção da imagem.
  const classeImagem = `${ALTURAS[tamanho]} w-auto`;
  const sizes = tamanho === "pequeno" ? "120px" : "260px";
  const trocaNoEscuro = corEscuro && corEscuro !== cor;

  return (
    // items-start: sem isso o flex estica a imagem até a largura do slogan (e deforma o logo).
    <span className="inline-flex flex-col items-start gap-2">
      {/* Com corEscuro, as duas versões ficam no HTML e o CSS mostra a do tema atual (não "pisca"). */}
      <Image
        src={IMAGENS[cor]}
        alt="sportfy."
        priority={prioridade}
        className={`${classeImagem} ${trocaNoEscuro ? "dark:hidden" : ""}`}
        sizes={sizes}
      />
      {trocaNoEscuro && (
        <Image
          src={IMAGENS[corEscuro]}
          alt="sportfy."
          priority={prioridade}
          className={`${classeImagem} hidden dark:block`}
          sizes={sizes}
        />
      )}
      {comSlogan && (
        <span className={sloganClaro ? "text-white/85" : "text-texto-suave"}>
          Pratique, Conecte e Divirta-se
        </span>
      )}
    </span>
  );
}
