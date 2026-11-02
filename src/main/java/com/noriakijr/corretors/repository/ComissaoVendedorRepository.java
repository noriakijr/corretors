package com.noriakijr.corretors.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.noriakijr.corretors.model.ComissaoVendedor;

public interface ComissaoVendedorRepository extends JpaRepository<ComissaoVendedor, Long> {

	@Query("select c from ComissaoVendedor c join fetch c.vendedor join fetch c.plano")
	List<ComissaoVendedor> findAllWithVendedor();

	Optional<ComissaoVendedor> findByPlanoIdAndVendedorId(Long planoId, Long vendedorId);

}
