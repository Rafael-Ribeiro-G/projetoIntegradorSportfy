/** Regra da API: 8+ caracteres, com pelo menos uma letra e um número. */
export const SENHA_VALIDA = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
export const MENSAGEM_SENHA = "A senha precisa ter 8 ou mais caracteres, com letra e número.";

export const EMAIL_VALIDO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
