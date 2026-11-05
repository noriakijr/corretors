package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;

public record DashboardContratoResponse(
		Long id,
		String codigo,
		Long titularId,
		String titularNome,
		String vendedorNome,
		LocalDateTime dtAquisicao,
		LocalDateTime dtFim,
		ContratoStatus status) {

	public static DashboardContratoResponse from(Contrato contrato) {
		return new DashboardContratoResponse(
				contrato.getId(),
				contrato.getCodigo(),
				contrato.getTitular() != null ? contrato.getTitular().getId() : null,
				contrato.getTitular() != null ? contrato.getTitular().getNome() : null,
				contrato.getVendedor() != null ? contrato.getVendedor().getNome() : null,
				contrato.getDtAquisicao(),
				contrato.getDtFim(),
				contrato.getStatus());
	}
}
