package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.PagamentoType;

import jakarta.validation.constraints.NotNull;

public record ContratoRequest(
		@NotNull Long titularId,
		@NotNull Long vendedorId,
		@NotNull Long planoId,
		@NotNull Float valor,
		String codigo,
		String descricao,
		LocalDateTime dtAquisicao,
		LocalDateTime dtInicio,
		LocalDateTime dtFim,
		String pagamentoDescricao,
		PagamentoType tipoPag,
		Double vlrDesconto) {
}
