package br.senai.sportfy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.service.EsporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/esportes")
@Tag(name = "Esportes", description = "Lista de esportes (somente leitura; cadastro feito no banco)")
public class EsporteController {

    private final EsporteService esporteService;

    public EsporteController(EsporteService esporteService) {
        this.esporteService = esporteService;
    }

    @GetMapping
    @Operation(summary = "Lista os esportes (para filtros e para cadastrar quadras)")
    public List<EsporteResponse> listar() {
        return esporteService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um esporte")
    public EsporteResponse buscar(@PathVariable Integer id) {
        return esporteService.buscar(id);
    }
}
