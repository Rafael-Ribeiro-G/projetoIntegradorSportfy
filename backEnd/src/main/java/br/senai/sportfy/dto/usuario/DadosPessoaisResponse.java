package br.senai.sportfy.dto.usuario;

import java.time.LocalDateTime;

import br.senai.sportfy.dto.evento.MeusEventosResponse;

/**
 * LGPD (art. 18): exportação de todos os dados pessoais que o Sportfy guarda sobre o titular.
 */
public record DadosPessoaisResponse(
        LocalDateTime geradoEm,
        UsuarioResponse dadosCadastrais,
        MeusEventosResponse eventos,
        String finalidade) {
}
