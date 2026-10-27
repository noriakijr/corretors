package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;

public record ContratoResponse(
		Long id,
		String codigo,
		String descricao,
		LocalDateTime dtAquisicao,
		LocalDateTime dtInicio,
		LocalDateTime dtFim,
		ContratoStatus status,
		Long titularId,
		Long vendedorId,
		Long planoHistoricoId,
		Long planoId,
		Float valor,
		Long pagamentoId,
		Long contratoAnteriorId) {

	public static ContratoResponse from(Contrato contrato) {
		Long planoHistoricoId = contrato.getPlanoHistorico() != null ? contrato.getPlanoHistorico().getId() : null;
		Long planoId = contrato.getPlanoHistorico() != null && contrato.getPlanoHistorico().getPlano() != null
				? contrato.getPlanoHistorico().getPlano().getId()
				: null;
		Float valor = contrato.getPlanoHistorico() != null ? contrato.getPlanoHistorico().getValor() : null;
		return new ContratoResponse(
				contrato.getId(),
				contrato.getCodigo(),
				contrato.getDescricao(),
				contrato.getDtAquisicao(),
				contrato.getDtInicio(),
				contrato.getDtFim(),
				contrato.getStatus(),
				contrato.getTitular() != null ? contrato.getTitular().getId() : null,
				contrato.getVendedor() != null ? contrato.getVendedor().getId() : null,
				planoHistoricoId,
				planoId,
				valor,
				contrato.getPagamento() != null ? contrato.getPagamento().getId() : null,
				contrato.getContratoAnterior() != null ? contrato.getContratoAnterior().getId() : null);
	}

}
