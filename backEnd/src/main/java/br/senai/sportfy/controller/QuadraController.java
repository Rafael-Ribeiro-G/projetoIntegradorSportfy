package br.senai.sportfy.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

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

import br.senai.sportfy.dto.quadra.QuadraRequest;
import br.senai.sportfy.dto.quadra.QuadraResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.QuadraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/quadras")
@Tag(name = "Quadras", description = "Quadras de cada local (cada quadra é de um esporte)")
public class QuadraController {

    private final QuadraService quadraService;

    public QuadraController(QuadraService quadraService) {
        this.quadraService = quadraService;
    }

    @GetMapping
    @Operation(summary = "Lista quadras, com filtro opcional por local e por esporte")
    public List<QuadraResponse> listar(@RequestParam(required = false) UUID localId,
                                       @RequestParam(required = false) Integer esporteId) {
        return quadraService.listar(localId, esporteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da quadra")
    public QuadraResponse detalhe(@PathVariable UUID id) {
        return quadraService.detalhe(id);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cadastra uma quadra em um local (somente o dono do local)")
    public ResponseEntity<QuadraResponse> criar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                                @Valid @RequestBody QuadraRequest request) {
        QuadraResponse criada = quadraService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/api/quadras/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita nome, esporte e preço da quadra (somente o dono do local; localId é ignorado)")
    public QuadraResponse atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable UUID id, @Valid @RequestBody QuadraRequest request) {
        return quadraService.atualizar(id, request, usuario.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui a quadra (somente o dono do local; recusado se houver partidas)")
    public void excluir(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                        @PathVariable UUID id) {
        quadraService.excluir(id, usuario.id());
    }
}
