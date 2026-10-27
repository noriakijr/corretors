package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

public record RenovarContratoRequest(
		LocalDateTime dtInicio,
		LocalDateTime dtFim,
		Float valor,
		String codigo,
		String descricao) {
}
