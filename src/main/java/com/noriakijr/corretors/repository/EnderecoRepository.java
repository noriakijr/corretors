package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Endereco;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
}
