package com.noriakijr.corretors.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

	List<Contrato> findByTitularIdOrderByDtAquisicaoDesc(Long titularId);

	List<Contrato> findByTitularIdAndStatus(Long titularId, ContratoStatus status);

	@Query("""
			select distinct c from Contrato c
			left join fetch c.titular
			left join fetch c.vendedor
			left join fetch c.pagamento
			left join fetch c.planoHistorico h
			left join fetch h.plano p
			left join fetch p.seguradora
			left join fetch c.contratoAnterior
			""")
	List<Contrato> findAllForRelatorio();

	@Query("""
			select c from Contrato c
			left join fetch c.titular
			left join fetch c.vendedor
			where c.pagamento.id = :pagamentoId
			""")
	Optional<Contrato> findByPagamentoId(@Param("pagamentoId") Long pagamentoId);

}
