package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DependenteRequest(
		@NotBlank(message = "informe o nome") String nome,
		@NotBlank(message = "informe o parentesco") String parentesco,
		@NotNull(message = "informe a data de nascimento") LocalDate dtNasc) {
}
