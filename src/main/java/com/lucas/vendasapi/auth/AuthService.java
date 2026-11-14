package com.lucas.vendasapi.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lucas.vendasapi.exception.ConflitoException;
import com.lucas.vendasapi.security.JwtService;
import com.lucas.vendasapi.usuario.Role;
import com.lucas.vendasapi.usuario.Usuario;
import com.lucas.vendasapi.usuario.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Cadastro aberto sempre cria um VENDEDOR; o ADMIN vem da configuração (AdminSeeder). */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário com o e-mail informado.");
        }
        Usuario usuario = new Usuario(
                request.nome().trim(), email, passwordEncoder.encode(request.senha()), Role.VENDEDOR);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /** Valida e-mail e senha; se estiverem errados o Spring Security lança AuthenticationException (401). */
    public TokenResponse login(LoginRequest request) {
        Authentication autenticacao = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.senha()));
        String token = jwtService.gerarToken(autenticacao.getName());
        return TokenResponse.bearer(token, jwtService.getExpiracaoEmSegundos());
    }
}
