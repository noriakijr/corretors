package com.noriakijr.corretors.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.Parcela;

public interface ParcelaRepository extends JpaRepository<Parcela, Long> {

	List<Parcela> findByPagamentoIdOrderByDtParcelaAscIdAsc(Long pagamentoId);
}
