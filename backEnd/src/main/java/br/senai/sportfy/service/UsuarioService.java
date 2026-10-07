package br.senai.sportfy.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Esporte;
import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.NivelHabilidade;
import br.senai.sportfy.domain.enums.Perfil;
import br.senai.sportfy.dto.PaginaResponse;
import br.senai.sportfy.dto.auth.TokenResponse;
import br.senai.sportfy.dto.usuario.AlterarSenhaRequest;
import br.senai.sportfy.dto.usuario.AtualizarPerfilRequest;
import br.senai.sportfy.dto.usuario.DadosPessoaisResponse;
import br.senai.sportfy.dto.usuario.PerfilPublicoResponse;
import br.senai.sportfy.dto.usuario.UsuarioResponse;
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
            + "organizar/participar de eventos esportivos. E-mail e telefone não são exibidos a outros usuários.";

    private final UsuarioRepository usuarioRepository;
    private final EsporteRepository esporteRepository;
    private final EventoService eventoService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarioRepository, EsporteRepository esporteRepository,
                          EventoService eventoService, PasswordEncoder passwordEncoder, JwtService jwtService,
                          Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.esporteRepository = esporteRepository;
        this.eventoService = eventoService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    // ------------------------------------------------------------ próprio usuário

    @Transactional(readOnly = true)
    public UsuarioResponse meuPerfil(Long usuarioId) {
        return UsuarioResponse.de(buscarEntidade(usuarioId));
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(Long usuarioId, AtualizarPerfilRequest req) {
        EventoService.validarCoordenadas(req.latitude(), req.longitude());
        Usuario usuario = buscarEntidade(usuarioId);

        usuario.setNome(req.nome().trim());
        usuario.setTelefone(AuthService.textoOuNulo(req.telefone()));
        usuario.setDataNascimento(req.dataNascimento());
        usuario.setCidade(AuthService.textoOuNulo(req.cidade()));
        usuario.setBio(AuthService.textoOuNulo(req.bio()));
        // "TODOS" faz sentido para evento, não para pessoa
        usuario.setNivel(req.nivel() == NivelHabilidade.TODOS ? null : req.nivel());
        usuario.setLatitude(req.latitude());
        usuario.setLongitude(req.longitude());

        if (req.esportesFavoritosIds() != null) {
            Set<Long> ids = new HashSet<>(req.esportesFavoritosIds());
            List<Esporte> esportes = esporteRepository.findAllById(ids);
            if (esportes.size() != ids.size()) {
                throw new RecursoNaoEncontradoException("Um ou mais esportes informados não existem.");
            }
            usuario.getEsportesFavoritos().clear();
            usuario.getEsportesFavoritos().addAll(esportes);
        }
        return UsuarioResponse.de(usuario);
    }

    /** Troca a senha e devolve um token novo (os tokens antigos deixam de valer). */
    @Transactional
    public TokenResponse alterarSenha(Long usuarioId, AlterarSenhaRequest req) {
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
     * LGPD (direito de eliminação): anonimiza os dados pessoais e desativa a conta.
     * O histórico de eventos passados é mantido, mas sem identificar a pessoa.
     */
    @Transactional
    public void excluirConta(Long usuarioId, String senha) {
        Usuario usuario = buscarEntidade(usuarioId);
        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        eventoService.encerrarVinculosFuturos(usuarioId);

        usuario.setNome("Usuário removido");
        usuario.setEmail("removido-" + usuarioId + "-" + UUID.randomUUID() + "@sportfy.invalid");
        usuario.setSenhaHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setTelefone(null);
        usuario.setDataNascimento(null);
        usuario.setCidade(null);
        usuario.setBio(null);
        usuario.setNivel(null);
        usuario.setLatitude(null);
        usuario.setLongitude(null);
        usuario.getEsportesFavoritos().clear();
        usuario.setAtivo(false);
        log.info("Conta anonimizada a pedido do titular: id={}", usuarioId);
    }

    /** LGPD (direito de acesso/portabilidade): todos os dados do titular em JSON. */
    @Transactional(readOnly = true)
    public DadosPessoaisResponse exportarDados(Long usuarioId) {
        Usuario usuario = buscarEntidade(usuarioId);
        return new DadosPessoaisResponse(LocalDateTime.now(clock), UsuarioResponse.de(usuario),
                eventoService.meusEventos(usuarioId), FINALIDADE_LGPD);
    }

    // ------------------------------------------------------------ outros usuários

    @Transactional(readOnly = true)
    public PerfilPublicoResponse perfilPublico(Long id) {
        Usuario usuario = buscarEntidade(id);
        if (!usuario.isAtivo()) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado.");
        }
        return PerfilPublicoResponse.de(usuario);
    }

    // ------------------------------------------------------------ administração

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String busca, Pageable pageable) {
        Page<Usuario> pagina = (busca == null || busca.isBlank())
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        busca.trim(), busca.trim(), pageable);
        return PaginaResponse.de(pagina, pagina.getContent().stream().map(UsuarioResponse::de).toList());
    }

    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo, Long adminId) {
        if (id.equals(adminId) && !ativo) {
            throw new RegraNegocioException("Você não pode desativar a sua própria conta por aqui.");
        }
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(ativo);
        log.info("Admin {} alterou status do usuário {} para ativo={}", adminId, id, ativo);
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse alterarPerfil(Long id, Perfil perfil, Long adminId) {
        if (id.equals(adminId) && perfil != Perfil.ADMIN) {
            throw new RegraNegocioException("Você não pode remover o seu próprio acesso de administrador.");
        }
        Usuario usuario = buscarEntidade(id);
        usuario.setPerfil(perfil);
        log.info("Admin {} alterou perfil do usuário {} para {}", adminId, id, perfil);
        return UsuarioResponse.de(usuario);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
}
