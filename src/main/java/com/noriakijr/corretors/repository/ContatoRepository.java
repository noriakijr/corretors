package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Contato;

public interface ContatoRepository extends JpaRepository<Contato, Long> {
}
