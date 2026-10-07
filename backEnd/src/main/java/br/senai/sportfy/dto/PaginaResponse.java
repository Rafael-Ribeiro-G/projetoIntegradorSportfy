package br.senai.sportfy.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/** Página de resultados em formato estável para o front e o mobile. */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima) {

    public static <T> PaginaResponse<T> de(Page<?> page, List<T> conteudo) {
        return new PaginaResponse<>(conteudo, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
