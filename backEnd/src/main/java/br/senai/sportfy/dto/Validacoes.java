package br.senai.sportfy.dto;

/**
 * Expressões regulares reutilizadas nas validações dos DTOs.
 */
public final class Validacoes {

    /** Bloqueia os caracteres "<" e ">" para evitar injeção de HTML/script (XSS) em campos de texto. */
    public static final String TEXTO_SEGURO = "^[^<>]*$";
    public static final String MSG_TEXTO_SEGURO = "não pode conter os caracteres < ou >";

    /** Pelo menos uma letra e um número. */
    public static final String SENHA_FORTE = "^(?=.*[A-Za-z])(?=.*\\d).+$";
    public static final String MSG_SENHA_FORTE = "deve conter pelo menos uma letra e um número";

    /** Dígitos, espaço, parênteses, hífen e + opcional no início. Ex: (11) 98888-7777 */
    public static final String TELEFONE = "^\\+?[0-9 ()-]{8,20}$";
    public static final String MSG_TELEFONE = "formato de telefone inválido";

    /** Link de imagem: http(s), sem espaços e sem < > (até 500 caracteres). */
    public static final String URL_IMAGEM = "^https?://[^\\s<>\"]{1,490}$";
    public static final String MSG_URL_IMAGEM = "deve ser um link http(s) válido, sem espaços";

    private Validacoes() {
    }
}
