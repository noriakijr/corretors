package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.noriakijr.corretors.model.PagamentoType;
import com.noriakijr.corretors.web.FlexibleLocalDateTimeDeserializer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

public record ContratoRequest(
		@NotNull Long titularId,
		@NotNull Long vendedorId,
		@NotNull Long planoId,
		@NotNull Float valor,
		String codigo,
		String descricao,
		LocalDateTime dtAquisicao,
		@JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime dtInicio,
		@JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime dtFim,
		String pagamentoDescricao,
		PagamentoType tipoPag,
		Double vlrDesconto,
		@Min(1) Integer quantidadeParcelas,
		@Valid List<DependenteRequest> dependentes) {
}
