package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.Min;

public record PlanoLimiteRequest(
		@Min(value = 0, message = "limite de dependentes invalido") Integer limiteDependentes) {
}
