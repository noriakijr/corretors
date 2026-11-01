package com.noriakijr.corretors.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noriakijr.corretors.model.PlanoHistorico;

public interface PlanoHistoricoRepository extends JpaRepository<PlanoHistorico, Long> {

	List<PlanoHistorico> findByCatalogoTrueOrderByDtModifDescIdDesc();

	List<PlanoHistorico> findByPlanoIdAndCatalogoTrueOrderByDtModifDescIdDesc(Long planoId);

}
