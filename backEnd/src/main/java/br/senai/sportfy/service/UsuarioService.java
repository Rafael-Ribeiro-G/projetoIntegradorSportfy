package br.senai.sportfy.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.dto.auth.TokenResponse;
import br.senai.sportfy.dto.usuario.AlterarSenhaRequest;
import br.senai.sportfy.dto.usuario.AtualizarPerfilRequest;
import br.senai.sportfy.dto.usuario.DadosPessoaisResponse;
import br.senai.sportfy.dto.usuario.PerfilPublicoResponse;
import br.senai.sportfy.dto.usuario.UsuarioResponse;
import br.senai.sportfy.exception.ConflitoException;
import br.senai.sportfy.exception.CredenciaisInvalidasException;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RegraNegocioException;
import br.senai.sportfy.repository.EsporteRepository;
import br.senai.sportfy.repository.UsuarioRepository;
import br.senai.sportfy.security.JwtService;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    static final String FINALIDADE_LGPD = "Os dados são usados apenas para criar sua conta, permitir login e "
            + "organizar/participar de partidas. E-mail e telefone não são exibidos a outros usuários.";

    private final UsuarioRepository usuarioRepository;
    private final EsporteRepository esporteRepository;
    private final PartidaService partidaService;
    private final LocalService localService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarioRepository, EsporteRepository esporteRepository,
                          PartidaService partidaService,
                          LocalService localService, PasswordEncoder passwordEncoder, JwtService jwtService,
                          Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.esporteRepository = esporteRepository;
        this.partidaService = partidaService;
        this.localService = localService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil(UUID usuarioId) {
        return UsuarioResponse.de(buscarEntidade(usuarioId));
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(UUID usuarioId, AtualizarPerfilRequest req) {
        Usuario usuario = buscarEntidade(usuarioId);
        usuario.setNome(req.nome().trim());
        usuario.setTelefone(AuthService.textoOuNulo(req.telefone()));
        usuario.setNivelHabilidade(req.nivelHabilidade());
        usuario.setFotoUrl(AuthService.textoOuNulo(req.fotoUrl()));
        usuario.setBio(AuthService.textoOuNulo(req.bio()));
        if (req.esportesFavoritosIds() != null) {
            usuario.getEsportesFavoritos().clear();
            usuario.getEsportesFavoritos().addAll(
                    AuthService.buscarEsportes(esporteRepository, req.esportesFavoritosIds()));
        }
        return UsuarioResponse.de(usuario);
    }

    /** Troca a senha e devolve um token novo (os tokens antigos deixam de valer). */
    @Transactional
    public TokenResponse alterarSenha(UUID usuarioId, AlterarSenhaRequest req) {
        Usuario usuario = buscarEntidade(usuarioId);
        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        if (passwordEncoder.matches(req.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
        return new TokenResponse(jwtService.gerarToken(usuario), "Bearer", jwtService.getExpiracaoSegundos(),
                UsuarioResponse.de(usuario));
    }

    /**
     * LGPD (direito de eliminação): apaga a conta. Pelas regras do banco, também são apagadas
     * as partidas que a pessoa criou e as participações dela; locais cadastrados por ela ficam sem dono.
     */
    @Transactional
    public void excluirConta(UUID usuarioId, String senha) {
        Usuario usuario = buscarEntidade(usuarioId);
        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // ex.: organizador/capitão de campeonato (essas tabelas não apagam em cascata)
            throw new ConflitoException("Sua conta está ligada a campeonatos ou equipes e não pode ser excluída "
                    + "agora. Fale com a administração do Sportfy.");
        }
        log.info("Conta excluída a pedido do titular: id={}", usuarioId);
    }

    /** LGPD (direito de acesso/portabilidade): todos os dados do titular em JSON. */
    @Transactional(readOnly = true)
    public DadosPessoaisResponse exportarDados(UUID usuarioId) {
        Usuario usuario = buscarEntidade(usuarioId);
        return new DadosPessoaisResponse(LocalDateTime.now(clock), UsuarioResponse.de(usuario),
                partidaService.minhasPartidas(usuarioId), localService.doUsuario(usuarioId), FINALIDADE_LGPD);
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponse perfilPublico(UUID id) {
        return PerfilPublicoResponse.de(buscarEntidade(id));
    }

    private Usuario buscarEntidade(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}
