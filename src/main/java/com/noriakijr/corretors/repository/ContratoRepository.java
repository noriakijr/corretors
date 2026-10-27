package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Contrato;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
}
