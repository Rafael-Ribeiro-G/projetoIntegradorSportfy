package br.senai.sportfy.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.security.JwtAuthenticationFilter;
import br.senai.sportfy.security.JwtService;
import br.senai.sportfy.security.RespostaErroJson;

/**
 * Regras de acesso da API.
 *
 * Público: cadastro/login, consultas de esportes, locais, quadras e partidas, health check e Swagger.
 * Logado: todo o resto (criar/editar, participar, perfil).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ROTAS_SWAGGER = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UsuarioRepository usuarioRepository,
                                                   ObjectMapper objectMapper) throws Exception {

        JwtAuthenticationFilter filtroJwt = new JwtAuthenticationFilter(jwtService, usuarioRepository);
        RespostaErroJson respostaErro = new RespostaErroJson(objectMapper);

        http
                // API stateless com token: não usa cookie de sessão, então CSRF não se aplica
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; img-src 'self' data:; "
                                        + "style-src 'self' 'unsafe-inline'; script-src 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(ROTAS_SWAGGER).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/esportes", "/api/esportes/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/locais", "/api/locais/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/quadras", "/api/quadras/{id}").permitAll()
                        // sugestões precisa vir ANTES de /api/partidas/{id}, senão casaria com o {id}
                        .requestMatchers(HttpMethod.GET, "/api/partidas/sugestoes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/partidas", "/api/partidas/{id}").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: hash com salt aleatório; a senha original nunca é gravada
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SportfyProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(properties.cors().listaOrigens());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
