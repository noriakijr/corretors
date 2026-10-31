package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Plano;

public record PlanoResponse(Long id, String nome, String descricao, Float valor) {

	public static PlanoResponse from(Plano plano, Float valor) {
		return new PlanoResponse(plano.getId(), plano.getNome(), plano.getDescricao(), valor);
	}

}
