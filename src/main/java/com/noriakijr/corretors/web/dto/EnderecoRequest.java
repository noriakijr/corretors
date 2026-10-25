package com.noriakijr.corretors.web.dto;

public record EnderecoRequest(
		String pais,
		String estado,
		String cidade,
		String bairro,
		String rua,
		String numero,
		String latitude,
		String longitude) {
}
