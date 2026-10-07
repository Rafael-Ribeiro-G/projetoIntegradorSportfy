package br.senai.sportfy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.dto.auth.TokenResponse;
import br.senai.sportfy.dto.evento.MeusEventosResponse;
import br.senai.sportfy.dto.usuario.AlterarSenhaRequest;
import br.senai.sportfy.dto.usuario.AtualizarPerfilRequest;
import br.senai.sportfy.dto.usuario.ConfirmarSenhaRequest;
import br.senai.sportfy.dto.usuario.DadosPessoaisResponse;
import br.senai.sportfy.dto.usuario.PerfilPublicoResponse;
import br.senai.sportfy.dto.usuario.UsuarioResponse;
import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.EventoService;
import br.senai.sportfy.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Tela Conta/Perfil")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final EventoService eventoService;

    public UsuarioController(UsuarioService usuarioService, EventoService eventoService) {
        this.usuarioService = usuarioService;
        this.eventoService = eventoService;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public UsuarioResponse meuPerfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.meuPerfil(usuario.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza o perfil (nome, telefone, cidade, bio, nível, localização e esportes favoritos)")
    public UsuarioResponse atualizar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @Valid @RequestBody AtualizarPerfilRequest request) {
        return usuarioService.atualizarPerfil(usuario.id(), request);
    }

    @PutMapping("/me/senha")
    @Operation(summary = "Altera a senha (exige a senha atual). Devolve um token novo; os antigos deixam de valer")
    public TokenResponse alterarSenha(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                                      @Valid @RequestBody AlterarSenhaRequest request) {
        return usuarioService.alterarSenha(usuario.id(), request);
    }

    @GetMapping("/me/eventos")
    @Operation(summary = "Eventos que o usuário organiza e eventos em que está confirmado")
    public MeusEventosResponse meusEventos(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return eventoService.meusEventos(usuario.id());
    }

    @GetMapping("/me/dados")
    @Operation(summary = "LGPD: exporta todos os dados pessoais do usuário")
    public DadosPessoaisResponse exportarDados(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.exportarDados(usuario.id());
    }

    @PostMapping("/me/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "LGPD: exclui (anonimiza) a conta. Exige confirmação da senha")
    public void excluirConta(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                             @Valid @RequestBody ConfirmarSenhaRequest request) {
        usuarioService.excluirConta(usuario.id(), request.senha());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Perfil público de outro usuário (sem e-mail/telefone)")
    public PerfilPublicoResponse perfilPublico(@PathVariable Long id) {
        return usuarioService.perfilPublico(id);
    }
}
