package br.senai.sportfy.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.senai.sportfy.security.UsuarioAutenticado;
import br.senai.sportfy.service.SportiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Chat do SportIA: repassa a pergunta do usuário logado ao serviço Python, sem expor a chave da Groq. */
@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "SportIA", description = "Assistente virtual (chatbot) do Sportfy")
@SecurityRequirement(name = "bearerAuth")
public class SportiaController {

    public record Mensagem(@NotBlank @Size(max = 1000) String pergunta) {
    }

    public record Resposta(String resposta) {
    }

    private final SportiaService sportiaService;

    public SportiaController(SportiaService sportiaService) {
        this.sportiaService = sportiaService;
    }

    @PostMapping("/mensagem")
    @Operation(summary = "Envia uma pergunta ao SportIA e devolve a resposta (até 1000 caracteres, "
            + SportiaService.LIMITE_PERGUNTAS + " perguntas a cada 10 min por usuário)")
    public Resposta enviar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario,
                           @Valid @RequestBody Mensagem mensagem) {
        return new Resposta(sportiaService.perguntar(usuario.id(), mensagem.pergunta()));
    }
}
