package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Titular;

public interface TitularRepository extends JpaRepository<Titular, Long> {
}
