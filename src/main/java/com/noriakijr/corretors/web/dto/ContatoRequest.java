package com.noriakijr.corretors.web.dto;

public record ContatoRequest(
		String telFixo,
		String telComercial,
		String telFax,
		String celular,
		String email,
		EnderecoRequest endereco) {
}
