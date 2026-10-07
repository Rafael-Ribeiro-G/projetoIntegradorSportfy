package br.senai.sportfy.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê o header "Authorization: Bearer <token>" e, se o token for válido e o usuário
 * estiver ativo, marca a requisição como autenticada.
 *
 * Não é um @Component de propósito: é criado dentro do SecurityConfig para não
 * ser registrado duas vezes como filtro do servlet.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIXO.length()).trim();
            jwtService.validar(token).ifPresent(info -> usuarioRepository.findById(info.usuarioId())
                    // conta desativada ou senha trocada depois da emissão do token => token não vale mais
                    .filter(Usuario::isAtivo)
                    .filter(usuario -> jwtService.versaoSenha(usuario).equals(info.versaoSenha()))
                    .ifPresent(usuario -> autenticar(usuario, request)));
        }
        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario, HttpServletRequest request) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getPerfil());
        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
    }
}
