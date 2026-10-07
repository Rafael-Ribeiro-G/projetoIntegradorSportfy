package br.senai.sportfy.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.domain.enums.Perfil;
import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.esporte.EsporteResponse;
import br.senai.sportfy.dto.usuario.UsuarioResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.EsporteService;
import br.senai.sportfy.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Administração", description = "Rotas exclusivas do perfil ADMIN")
public class AdminController {

    private final UsuarioService usuarioService;
    private final EsporteService esporteService;

    public AdminController(UsuarioService usuarioService, EsporteService esporteService) {
        this.usuarioService = usuarioService;
        this.esporteService = esporteService;
    }

    public record StatusRequest(@NotNull Boolean ativo) {
    }

    public record PerfilRequest(@NotNull Perfil perfil) {
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Lista usuários (busca por nome ou e-mail)")
    public PaginaResponse<UsuarioResponse> listarUsuarios(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return usuarioService.listar(busca, PageRequest.of(pagina, tamanho, Sort.by("nome")));
    }

    @PatchMapping("/usuarios/{id}/status")
    @Operation(summary = "Ativa ou desativa (bloqueia) um usuário")
    public UsuarioResponse alterarStatus(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado admin,
                                         @PathVariable Long id,
                                         @Valid @RequestBody StatusRequest request) {
        return usuarioService.alterarStatus(id, request.ativo(), admin.id());
    }

    @PatchMapping("/usuarios/{id}/perfil")
    @Operation(summary = "Promove a ADMIN ou volta para USER")
    public UsuarioResponse alterarPerfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado admin,
                                         @PathVariable Long id,
                                         @Valid @RequestBody PerfilRequest request) {
        return usuarioService.alterarPerfil(id, request.perfil(), admin.id());
    }

    @GetMapping("/esportes")
    @Operation(summary = "Lista todos os esportes, inclusive inativos")
    public List<EsporteResponse> listarEsportes() {
        return esporteService.listar(true);
    }
}
