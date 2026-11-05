package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.ParcelaStatus;

public record DashboardParcelaResponse(
		Long id,
		Long contratoId,
		String codigo,
		String titularNome,
		LocalDateTime dtParcela,
		Double valor,
		ParcelaStatus status) {

	public static DashboardParcelaResponse from(Parcela parcela, Contrato contrato, ParcelaStatus status) {
		return new DashboardParcelaResponse(
				parcela.getId(),
				contrato != null ? contrato.getId() : null,
				contrato != null ? contrato.getCodigo() : null,
				contrato != null && contrato.getTitular() != null ? contrato.getTitular().getNome() : null,
				parcela.getDtParcela(),
				parcela.getVlrParcela(),
				status);
	}
}
