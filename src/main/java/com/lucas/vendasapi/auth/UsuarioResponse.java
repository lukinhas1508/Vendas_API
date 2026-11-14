package com.lucas.vendasapi.auth;

import com.lucas.vendasapi.usuario.Role;
import com.lucas.vendasapi.usuario.Usuario;

public record UsuarioResponse(Long id, String nome, String email, Role role) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
