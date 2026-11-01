package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;

import com.noriakijr.corretors.model.Dependente;

public record DependenteResponse(
		Long id,
		String nome,
		String parentesco,
		LocalDate dtNasc,
		Long contratoId) {

	public static DependenteResponse from(Dependente dependente) {
		Long contratoId = dependente.getContrato() != null ? dependente.getContrato().getId() : null;
		return new DependenteResponse(
				dependente.getId(),
				dependente.getNome(),
				dependente.getParentesco(),
				dependente.getDtNasc(),
				contratoId);
	}

}
