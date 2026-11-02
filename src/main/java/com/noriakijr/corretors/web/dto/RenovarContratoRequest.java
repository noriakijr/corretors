package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.PositiveOrZero;

public record RenovarContratoRequest(
		LocalDateTime dtInicio,
		LocalDateTime dtFim,
		@PositiveOrZero(message = "valor da renovacao nao pode ser negativo") Float valor,
		String codigo,
		String descricao) {
}
