package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record TitularRequest(
		@NotBlank String nome,
		String cpf,
		String rg,
		LocalDate dtNasc,
		Boolean cartaoEnv,
		Boolean seguroVidaEnv,
		ContatoRequest contato) {
}
