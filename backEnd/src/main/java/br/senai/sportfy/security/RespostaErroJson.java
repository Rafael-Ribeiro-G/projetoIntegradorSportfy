package br.senai.sportfy.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.senai.sportfy.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Responde 401 e 403 no mesmo formato JSON do restante da API
 * (sem isso o Spring Security devolve uma resposta vazia).
 */
public class RespostaErroJson implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostaErroJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(response, request, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Envie um token válido no header Authorization.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(response, request, HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.");
    }

    private void escrever(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                          String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErroResponse corpo = ErroResponse.de(status.value(), status.getReasonPhrase(), mensagem,
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
