package br.senai.sportfy.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.senai.sportfy.domain.Usuario;
import br.senai.sportfy.domain.enums.Perfil;
import br.senai.sportfy.repository.UsuarioRepository;

/**
 * Cria o primeiro administrador na subida da API, SE as variáveis
 * ADMIN_EMAIL e ADMIN_SENHA estiverem definidas e o e-mail ainda não existir.
 * Assim nenhuma senha fica escrita no código.
 */
@Component
public class AdminInicialConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialConfig.class);

    private final SportfyProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInicialConfig(SportfyProperties properties, UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SportfyProperties.Admin admin = properties.admin();
        if (admin == null || isVazio(admin.email()) || isVazio(admin.senha())) {
            return;
        }
        String email = admin.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        if (admin.senha().length() < 8) {
            log.warn("ADMIN_SENHA precisa ter pelo menos 8 caracteres; administrador não foi criado.");
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome("Administrador");
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(admin.senha()));
        usuario.setPerfil(Perfil.ADMIN);
        usuario.setAtivo(true);
        usuario.setConsentimentoLgpdEm(LocalDateTime.now());
        usuarioRepository.save(usuario);
        log.info("Administrador inicial criado: {}", email);
    }

    private static boolean isVazio(String s) {
        return s == null || s.isBlank();
    }
}
