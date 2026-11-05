package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.ParcelaStatus;

public record FinanceiroParcelaResponse(
		Long id,
		Long contratoId,
		String codigo,
		String titularNome,
		LocalDateTime dtParcela,
		LocalDateTime dtPagamento,
		Double valor,
		ParcelaStatus status) {

	public static FinanceiroParcelaResponse from(Parcela parcela, Contrato contrato, LocalDate hoje) {
		return new FinanceiroParcelaResponse(
				parcela.getId(),
				contrato != null ? contrato.getId() : null,
				contrato != null ? contrato.getCodigo() : null,
				contrato != null && contrato.getTitular() != null ? contrato.getTitular().getNome() : null,
				parcela.getDtParcela(),
				parcela.getDtPagamento(),
				parcela.getVlrParcela(),
				ParcelaStatus.of(parcela.getDtParcela(), parcela.getDtPagamento(), hoje));
	}
}
