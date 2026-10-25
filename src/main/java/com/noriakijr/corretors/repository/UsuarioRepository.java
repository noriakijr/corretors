package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
