package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Plano;

public record PlanoResponse(
		Long id,
		String nome,
		String descricao,
		Float valor,
		Float valorRenovacao,
		Long seguradoraId,
		String seguradoraNome,
		Integer limiteDependentes) {

	public static PlanoResponse from(Plano plano, Float valor, Float valorRenovacao) {
		Long seguradoraId = plano.getSeguradora() != null ? plano.getSeguradora().getId() : null;
		String seguradoraNome = plano.getSeguradora() != null ? plano.getSeguradora().getNome() : null;
		return new PlanoResponse(plano.getId(), plano.getNome(), plano.getDescricao(), valor, valorRenovacao,
				seguradoraId, seguradoraNome, plano.getLimiteDependentes());
	}

}
