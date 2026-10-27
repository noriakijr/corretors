package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Contato;

public record ContatoResponse(
		Long id,
		String telFixo,
		String telComercial,
		String telFax,
		String celular,
		String email,
		EnderecoResponse endereco) {

	public static ContatoResponse from(Contato contato) {
		if (contato == null) {
			return null;
		}
		return new ContatoResponse(
				contato.getId(),
				contato.getTelFixo(),
				contato.getTelComercial(),
				contato.getTelFax(),
				contato.getCelular(),
				contato.getEmail(),
				EnderecoResponse.from(contato.getEndereco()));
	}

}
