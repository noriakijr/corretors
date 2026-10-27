package com.noriakijr.corretors.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

	List<Contrato> findByTitularIdOrderByDtAquisicaoDesc(Long titularId);

	List<Contrato> findByTitularIdAndStatus(Long titularId, ContratoStatus status);

}
