package br.senai.sportfy.dto.usuario;

import java.time.LocalDateTime;
import java.util.List;

import br.senai.sportfy.dto.local.LocalResponse;
import br.senai.sportfy.dto.partida.MinhasPartidasResponse;

/**
 * LGPD (art. 18): exportação de todos os dados pessoais que o Sportfy guarda sobre o titular.
 */
public record DadosPessoaisResponse(
        LocalDateTime geradoEm,
        UsuarioResponse dadosCadastrais,
        MinhasPartidasResponse partidas,
        List<LocalResponse> locaisCadastrados,
        String finalidade) {
}
