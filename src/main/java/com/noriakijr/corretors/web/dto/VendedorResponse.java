package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Vendedor;

public record VendedorResponse(Long id, String nome) {

	public static VendedorResponse from(Vendedor vendedor) {
		return new VendedorResponse(vendedor.getId(), vendedor.getNome());
	}

}
