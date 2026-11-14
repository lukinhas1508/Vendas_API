package com.lucas.vendasapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.usuario.Role;
import com.lucas.vendasapi.usuario.Usuario;
import com.lucas.vendasapi.usuario.UsuarioRepository;

/**
 * Cria o usuário administrador na primeira execução, a partir das variáveis
 * ADMIN_EMAIL e ADMIN_PASSWORD. Assim nenhuma credencial fica no código.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isBlank() || senha.isBlank()) {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD não definidos: nenhum administrador foi criado.");
            return;
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        usuarioRepository.save(new Usuario(
                "Administrador", email.trim().toLowerCase(), passwordEncoder.encode(senha), Role.ADMIN));
        log.info("Usuário administrador criado: {}", email);
    }
}
