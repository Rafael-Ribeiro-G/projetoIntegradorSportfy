package br.senai.sportfy.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import br.senai.sportfy.dto.ErroResponse;
import br.senai.sportfy.dto.ErroResponse.CampoErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Tratamento global de erros: toda exceção vira um JSON padronizado ({@link ErroResponse})
 * e detalhes internos (stack trace, SQL) nunca são enviados ao cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erros de negócio lançados pelos services (404, 409, 422, 401, 403, 429...). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApi(ApiException ex, HttpServletRequest req) {
        return responder(ex.getStatus(), ex.getMessage(), req, null);
    }

    /** Falha de validação no corpo (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoErro(e.getField(), e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Dados inválidos. Verifique os campos.", req, campos);
    }

    /** Falha de validação em parâmetros de URL/query (@Min, @Max etc. nos controllers). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> tratarValidacaoParametros(HandlerMethodValidationException ex,
                                                                   HttpServletRequest req) {
        List<CampoErro> campos = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new CampoErro(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<CampoErro> campos = ex.getConstraintViolations().stream()
                .map(v -> new CampoErro(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Parâmetros inválidos.", req, campos);
    }

    /** JSON malformado ou com tipo errado (ex.: texto em campo numérico, data em formato inválido). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Verifique o JSON e os formatos (datas: yyyy-MM-ddTHH:mm:ss).", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipo(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.", req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroFaltando(MissingServletRequestParameterException ex,
                                                                 HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'.",
                req, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> tratarArgumento(IllegalArgumentException ex, HttpServletRequest req) {
        log.debug("Argumento inválido: {}", ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "Requisição inválida.", req, null);
    }

    /** Violação de chave única/estrangeira que escapou das validações (ex.: cadastro simultâneo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "Operação conflita com dados já existentes.", req, null);
    }

    /** @PreAuthorize negado (AuthorizationDeniedException é subclasse de AccessDeniedException). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "Você não tem permissão para acessar este recurso.", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado nesta rota.", req, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo como application/json.", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "Rota não encontrada.", req, null);
    }

    /** Qualquer outro erro: loga o detalhe no servidor e devolve mensagem genérica. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex, HttpServletRequest req) {
        // erros do próprio Spring que já sabem seu status (406, 400 de header faltando, ResponseStatusException...)
        if (ex instanceof ErrorResponse erroSpring) {
            HttpStatus status = HttpStatus.resolve(erroSpring.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return responder(status, "Requisição inválida.", req, null);
            }
        }
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Tente novamente mais tarde.", req, null);
    }

    private ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem, HttpServletRequest req,
                                                   List<CampoErro> campos) {
        ErroResponse corpo = new ErroResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI(), campos);
        return ResponseEntity.status(status).body(corpo);
    }
}
