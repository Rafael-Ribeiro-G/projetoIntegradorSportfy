package br.senai.sportfy.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.dto.auth.LoginRequest;
import br.senai.sportfy.dto.auth.RegistroRequest;
import br.senai.sportfy.dto.auth.TokenResponse;
import br.senai.sportfy.dto.usuario.UsuarioResponse;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.CredenciaisInvalidasException;
import br.senai.sportfy.exception.MuitasTentativasException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.security.JwtService;
import br.senai.sportfy.security.LoginAttemptService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final EsporteRepository esporteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar quais e-mails existem. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, EsporteRepository esporteRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, LoginAttemptService loginAttemptService) {
        this.usuarioRepository = usuarioRepository;
        this.esporteRepository = esporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-sportfy-1");
    }

    @Transactional
    public TokenResponse registrar(RegistroRequest req) {
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuario.setTelefone(textoOuNulo(req.telefone()));
        usuario.setNivelHabilidade(req.nivelHabilidade());
        usuario.setFotoUrl(textoOuNulo(req.fotoUrl()));
        usuario.setBio(textoOuNulo(req.bio()));
        if (req.esportesFavoritosIds() != null) {
            usuario.getEsportesFavoritos().addAll(buscarEsportes(esporteRepository, req.esportesFavoritosIds()));
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Novo usuário cadastrado: id={}", usuario.getId());
        return gerarResposta(usuario);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = normalizarEmail(req.email());

        if (loginAttemptService.estaBloqueado(email)) {
            throw new MuitasTentativasException(loginAttemptService.getMinutosBloqueio());
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        String hash = usuario != null ? usuario.getSenhaHash() : hashFicticio;
        boolean senhaConfere = passwordEncoder.matches(req.senha(), hash);

        if (usuario == null || !senhaConfere) {
            loginAttemptService.registrarFalha(email);
            log.warn("Falha de login para o e-mail informado (tentativa registrada)");
            throw new CredenciaisInvalidasException();
        }

        loginAttemptService.registrarSucesso(email);
        return gerarResposta(usuario);
    }

    private TokenResponse gerarResposta(Usuario usuario) {
        String token = jwtService.gerarToken(usuario);
        return new TokenResponse(token, "Bearer", jwtService.getExpiracaoSegundos(), UsuarioResponse.de(usuario));
    }

    /** Busca os esportes pelos ids; 404 se algum não existir. */
    static List<Esporte> buscarEsportes(EsporteRepository repo, Collection<Integer> ids) {
        Set<Integer> unicos = new HashSet<>(ids);
        List<Esporte> esportes = repo.findAllById(unicos);
        if (esportes.size() != unicos.size()) {
            throw new RecursoNaoEncontradoException("Um ou mais esportes informados não existem.");
        }
        return esportes;
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    static String textoOuNulo(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
