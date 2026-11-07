// Separado de lib/tema.ts porque é importado pelo layout raiz (Server Component),
// que não pode importar hooks do React.

export const CHAVE_TEMA = "sportfy.tema";
export const CONSULTA_ESCURO = "(prefers-color-scheme: dark)";

/**
 * Script que roda no <head>, ANTES da página aparecer, para não "piscar" branco
 * em quem usa o modo escuro. Precisa ser texto puro (roda direto no navegador).
 */
export const SCRIPT_TEMA_INICIAL = `(function(){try{var p=localStorage.getItem("${CHAVE_TEMA}");var e=p==="escuro"||(p!=="claro"&&window.matchMedia("${CONSULTA_ESCURO}").matches);document.documentElement.dataset.tema=e?"escuro":"claro";}catch(_){document.documentElement.dataset.tema="claro";}})();`;
