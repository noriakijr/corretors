package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.PagamentoType;
import com.noriakijr.corretors.web.FlexibleLocalDateTimeDeserializer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.databind.annotation.JsonDeserialize;

public record RenovarContratoRequest(
		@JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime dtInicio,
		@JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime dtFim,
		@PositiveOrZero(message = "valor da renovacao nao pode ser negativo") Float valor,
		String codigo,
		String descricao,
		PagamentoType tipoPag,
		Double vlrDesconto,
		@Min(1) Integer quantidadeParcelas) {
}
