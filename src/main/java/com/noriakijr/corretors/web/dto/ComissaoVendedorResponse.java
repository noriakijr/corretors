package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.ComissaoVendedor;
import com.noriakijr.corretors.model.TipoComissao;

public record ComissaoVendedorResponse(
		Long vendedorId,
		String vendedorNome,
		TipoComissao tipoComissao,
		Double percentual,
		Double valorFixo) {

	public static ComissaoVendedorResponse from(ComissaoVendedor regra) {
		return new ComissaoVendedorResponse(
				regra.getVendedor().getId(),
				regra.getVendedor().getNome(),
				regra.getTipoComissao(),
				regra.getPercentual(),
				regra.getValorFixo());
	}

}
