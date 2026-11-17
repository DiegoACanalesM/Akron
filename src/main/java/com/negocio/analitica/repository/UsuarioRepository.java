package com.negocio.analitica.repository;

import com.negocio.analitica.model.Usuario;
import java.util.Optional;

/** Persistence contract for system users. */
public interface UsuarioRepository {
    Usuario save(Usuario user);
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByUsername(String username);
}
