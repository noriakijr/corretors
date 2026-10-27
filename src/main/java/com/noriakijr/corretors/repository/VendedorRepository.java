package com.noriakijr.corretors.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Vendedor;

public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
}
