package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Plano;

public interface PlanoRepository extends JpaRepository<Plano, Long> {
}
