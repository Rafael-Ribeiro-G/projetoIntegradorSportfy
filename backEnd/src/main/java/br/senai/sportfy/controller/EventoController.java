package br.senai.sportfy.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.evento.EventoFiltro;
import br.senai.sportfy.dto.evento.EventoRequest;
import br.senai.sportfy.dto.evento.EventoResponse;
import br.senai.sportfy.dto.evento.ParticipanteResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Telas Home, Ver Eventos e Criar Evento")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    @Operation(summary = "Busca eventos futuros com filtros opcionais e paginação",
            description = "Para busca por proximidade envie latitude, longitude e raioKm. "
                    + "Datas no formato 2026-10-20T19:00:00. Ordenado pela data do evento.")
    public PaginaResponse<EventoResponse> buscar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) Long esporteId,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) NivelHabilidade nivel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double raioKm,
            @RequestParam(defaultValue = "false") boolean incluirEncerrados,
            @RequestParam(defaultValue = "false") boolean incluirCancelados,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int tamanho) {

        EventoFiltro filtro = new EventoFiltro(esporteId, cidade, texto, nivel, dataInicio, dataFim,
                latitude, longitude, raioKm, incluirEncerrados, incluirCancelados);
        PageRequest pageable = PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Order.asc("dataHoraInicio"), Sort.Order.asc("id")));
        return eventoService.buscar(filtro, pageable, usuario == null ? null : usuario.id());
    }

    @GetMapping("/sugestoes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Sugestões de eventos para o usuário logado (esportes favoritos + proximidade). Usado pela Home e pelo chatbot")
    public List<EventoResponse> sugestoes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limite) {
        return eventoService.sugestoes(usuario.id(), limite);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um evento")
    public EventoResponse buscarPorId(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                      @PathVariable Long id) {
        return eventoService.buscarPorId(id, usuario == null ? null : usuario.id());
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cria um evento; quem cria vira organizador e já ocupa uma vaga")
    public ResponseEntity<EventoResponse> criar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Valid @RequestBody EventoRequest request) {
        EventoResponse criado = eventoService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/api/eventos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita um evento (somente organizador ou ADMIN, antes de começar)")
    public EventoResponse atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable Long id,
                                    @Valid @RequestBody EventoRequest request) {
        return eventoService.atualizar(id, request, usuario);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancela o evento (somente organizador ou ADMIN). O registro é mantido como CANCELADO")
    public void cancelar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                         @PathVariable Long id) {
        eventoService.cancelar(id, usuario);
    }

    @PostMapping("/{id}/participacao")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Confirma presença do usuário logado no evento")
    public EventoResponse participar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable Long id) {
        return eventoService.participar(id, usuario.id());
    }

    @DeleteMapping("/{id}/participacao")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desiste de participar do evento")
    public EventoResponse sair(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                               @PathVariable Long id) {
        return eventoService.sair(id, usuario.id());
    }

    @GetMapping("/{id}/participantes")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lista os participantes confirmados (somente usuários logados)")
    public List<ParticipanteResponse> participantes(@PathVariable Long id) {
        return eventoService.listarParticipantes(id);
    }
}
