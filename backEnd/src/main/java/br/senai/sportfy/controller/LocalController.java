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

import br.senai.sportfy.dto.local.LocalRequest;
import br.senai.sportfy.dto.local.LocalResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.LocalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locais")
@Tag(name = "Locais", description = "Lugares onde ficam as quadras (clubes, parques, escolas)")
public class LocalController {

    private final LocalService localService;

    public LocalController(LocalService localService) {
        this.localService = localService;
    }

    @GetMapping
    @Operation(summary = "Lista locais. 'texto' busca no nome e no endereço; com latitude/longitude ordena pelos mais próximos")
    public List<LocalResponse> buscar(@RequestParam(required = false) String texto,
                                      @RequestParam(required = false) Double latitude,
                                      @RequestParam(required = false) Double longitude,
                                      @RequestParam(required = false) Double raioKm) {
        return localService.buscar(texto, latitude, longitude, raioKm);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do local, com as quadras")
    public LocalResponse detalhe(@PathVariable UUID id) {
        return localService.detalhe(id);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cadastra um local; quem cadastra vira o dono")
    public ResponseEntity<LocalResponse> criar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                               @Valid @RequestBody LocalRequest request) {
        LocalResponse criado = localService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/api/locais/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita o local (somente o dono)")
    public LocalResponse atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                   @PathVariable UUID id, @Valid @RequestBody LocalRequest request) {
        return localService.atualizar(id, request, usuario.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui o local e suas quadras (somente o dono; recusado se houver partidas)")
    public void excluir(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                        @PathVariable UUID id) {
        localService.excluir(id, usuario.id());
    }
}
