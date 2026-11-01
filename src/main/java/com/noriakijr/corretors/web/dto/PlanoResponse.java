package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Plano;

public record PlanoResponse(Long id, String nome, String descricao, Float valor, Float valorRenovacao) {

	public static PlanoResponse from(Plano plano, Float valor, Float valorRenovacao) {
		return new PlanoResponse(plano.getId(), plano.getNome(), plano.getDescricao(), valor, valorRenovacao);
	}

}
