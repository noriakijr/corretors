package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Endereco;

public record EnderecoResponse(
		Long id,
		String pais,
		String estado,
		String cidade,
		String bairro,
		String rua,
		String numero,
		String latitude,
		String longitude) {

	public static EnderecoResponse from(Endereco endereco) {
		if (endereco == null) {
			return null;
		}
		return new EnderecoResponse(
				endereco.getId(),
				endereco.getPais(),
				endereco.getEstado(),
				endereco.getCidade(),
				endereco.getBairro(),
				endereco.getRua(),
				endereco.getNumero(),
				endereco.getLatitude(),
				endereco.getLongitude());
	}

}
