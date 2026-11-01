package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.Seguradora;

public record SeguradoraResponse(Long id, String nome) {

	public static SeguradoraResponse from(Seguradora seguradora) {
		return new SeguradoraResponse(seguradora.getId(), seguradora.getNome());
	}

}
