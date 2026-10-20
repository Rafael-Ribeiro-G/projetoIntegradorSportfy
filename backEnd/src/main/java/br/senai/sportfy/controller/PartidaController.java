package br.senai.sportfy.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.partida.ParticipanteResponse;
import br.senai.sportfy.dto.partida.PartidaFiltro;
import br.senai.sportfy.dto.partida.PartidaRequest;
import br.senai.sportfy.dto.partida.PartidaResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.PartidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/partidas")
@Tag(name = "Partidas", description = "Telas Home, Ver Partidas e Criar Partida")
public class PartidaController {

    private final PartidaService partidaService;

    public PartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    @GetMapping
    @Operation(summary = "Busca partidas (por padrão só as que ainda não terminaram e não foram canceladas)",
            description = "'texto' busca no nome do local, no endereço e no nome da quadra. Para proximidade "
                    + "envie latitude, longitude e raioKm. Datas no formato 2026-10-25T19:00:00. "
                    + "Ordenado pela data da partida.")
    public PaginaResponse<PartidaResponse> buscar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Integer esporteId,
            @RequestParam(required = false) UUID localId,
            @RequestParam(required = false) UUID quadraId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double raioKm,
            @RequestParam(defaultValue = "false") boolean incluirEncerradas,
            @RequestParam(defaultValue = "false") boolean incluirCanceladas,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho) {

        PartidaFiltro filtro = new PartidaFiltro(esporteId, localId, quadraId, texto, dataInicio, dataFim,
                latitude, longitude, raioKm, incluirEncerradas, incluirCanceladas);
        return partidaService.buscar(filtro, PageRequest.of(pagina, tamanho), usuario == null ? null : usuario.id());
    }

    @GetMapping("/sugestoes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Próximas partidas com vaga para o usuário logado (Home e chatbot). "
            + "Com latitude/longitude, as mais próximas vêm primeiro")
    public List<PartidaResponse> sugestoes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Integer esporteId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limite) {
        return partidaService.sugestoes(usuario.id(), esporteId, latitude, longitude, limite);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma partida")
    public PartidaResponse buscarPorId(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                       @PathVariable UUID id) {
        return partidaService.buscarPorId(id, usuario == null ? null : usuario.id());
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria uma partida numa quadra; quem cria já ocupa uma vaga")
    public ResponseEntity<PartidaResponse> criar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @Valid @RequestBody PartidaRequest request) {
        PartidaResponse criada = partidaService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/api/partidas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita a partida (somente quem criou, antes de começar)")
    public PartidaResponse atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody PartidaRequest request) {
        return partidaService.atualizar(id, request, usuario.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancela a partida (somente quem criou). O registro fica com status 'cancelada'")
    public void cancelar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                         @PathVariable UUID id) {
        partidaService.cancelar(id, usuario.id());
    }

    @PostMapping("/{id}/participacao")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Entra na partida (confirma presença do usuário logado)")
    public PartidaResponse participar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                      @PathVariable UUID id) {
        return partidaService.participar(id, usuario.id());
    }

    @DeleteMapping("/{id}/participacao")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Sai da partida")
    public PartidaResponse sair(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                @PathVariable UUID id) {
        return partidaService.sair(id, usuario.id());
    }

    @GetMapping("/{id}/participantes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lista os jogadores confirmados (somente usuários logados)")
    public List<ParticipanteResponse> participantes(@PathVariable UUID id) {
        return partidaService.listarParticipantes(id);
    }
}
