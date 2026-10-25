package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;

import com.noriakijr.corretors.model.Titular;

public record TitularResponse(
		Long id,
		String nome,
		String cpf,
		String rg,
		LocalDate dtNasc,
		Boolean cartaoEnv,
		Boolean seguroVidaEnv,
		ContatoResponse contato) {

	public static TitularResponse from(Titular titular) {
		return new TitularResponse(
				titular.getId(),
				titular.getNome(),
				titular.getCpf(),
				titular.getRg(),
				titular.getDtNasc(),
				titular.getCartaoEnv(),
				titular.getSeguroVidaEnv(),
				ContatoResponse.from(titular.getContato()));
	}

}
