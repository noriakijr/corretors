package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PlanoRequest(
		@NotBlank String nome,
		String descricao,
		@PositiveOrZero Float valor,
		@PositiveOrZero Float valorRenovacao,
		@Min(value = 0, message = "limite de dependentes invalido") Integer limiteDependentes) {
}
