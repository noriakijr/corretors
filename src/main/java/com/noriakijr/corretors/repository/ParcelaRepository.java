package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Parcela;

public interface ParcelaRepository extends JpaRepository<Parcela, Long> {
}
