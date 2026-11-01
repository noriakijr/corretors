package com.noriakijr.corretors.web.dto;

import java.util.List;

import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.TipoComissao;

public record PlanoResponse(
		Long id,
		String nome,
		String descricao,
		Float valor,
		Float valorRenovacao,
		Long seguradoraId,
		String seguradoraNome,
		Integer limiteDependentes,
		TipoComissao tipoComissao,
		Double percentual,
		Double valorFixo,
		List<ComissaoVendedorResponse> excecoes) {

	public static PlanoResponse from(
			Plano plano,
			Float valor,
			Float valorRenovacao,
			List<ComissaoVendedorResponse> excecoes) {
		Long seguradoraId = plano.getSeguradora() != null ? plano.getSeguradora().getId() : null;
		String seguradoraNome = plano.getSeguradora() != null ? plano.getSeguradora().getNome() : null;
		return new PlanoResponse(
				plano.getId(),
				plano.getNome(),
				plano.getDescricao(),
				valor,
				valorRenovacao,
				seguradoraId,
				seguradoraNome,
				plano.getLimiteDependentes(),
				plano.getTipoComissao(),
				plano.getPercentualComissao(),
				plano.getValorFixoComissao(),
				excecoes);
	}

}
