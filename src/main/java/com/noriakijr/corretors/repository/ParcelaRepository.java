package com.noriakijr.corretors.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.noriakijr.corretors.model.Parcela;

public interface ParcelaRepository extends JpaRepository<Parcela, Long> {

	List<Parcela> findByPagamentoIdOrderByDtParcelaAscIdAsc(Long pagamentoId);

	@Query("""
			select p from Parcela p
			join fetch p.pagamento
			order by p.dtParcela asc, p.id asc
			""")
	List<Parcela> findAllWithPagamento();
}
