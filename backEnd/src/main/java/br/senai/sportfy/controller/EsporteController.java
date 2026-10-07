package br.senai.sportfy.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.dto.esporte.EsporteRequest;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.service.EsporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/esportes")
@Tag(name = "Esportes", description = "Lista de esportes (leitura pública; escrita só ADMIN)")
public class EsporteController {

    private final EsporteService esporteService;

    public EsporteController(EsporteService esporteService) {
        this.esporteService = esporteService;
    }

    @GetMapping
    @Operation(summary = "Lista os esportes ativos (para os filtros e o formulário de evento)")
    public List<EsporteResponse> listar() {
        return esporteService.listar(false);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um esporte")
    public EsporteResponse buscar(@PathVariable Long id) {
        return esporteService.buscar(id);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ADMIN: cadastra um esporte")
    public ResponseEntity<EsporteResponse> criar(@Valid @RequestBody EsporteRequest request) {
        EsporteResponse criado = esporteService.criar(request);
        return ResponseEntity.created(URI.create("/api/esportes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ADMIN: atualiza um esporte")
    public EsporteResponse atualizar(@PathVariable Long id, @Valid @RequestBody EsporteRequest request) {
        return esporteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "ADMIN: remove um esporte (ou desativa, se já tiver eventos)")
    public void excluir(@PathVariable Long id) {
        esporteService.excluir(id);
    }
}
