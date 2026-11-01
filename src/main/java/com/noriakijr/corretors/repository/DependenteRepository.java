package com.noriakijr.corretors.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Dependente;

public interface DependenteRepository extends JpaRepository<Dependente, Long> {

	List<Dependente> findByContratoIdOrderByIdAsc(Long contratoId);

	long countByContratoId(Long contratoId);

	Optional<Dependente> findByIdAndContratoId(Long id, Long contratoId);

}
