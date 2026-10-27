package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Dependente;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {
}
