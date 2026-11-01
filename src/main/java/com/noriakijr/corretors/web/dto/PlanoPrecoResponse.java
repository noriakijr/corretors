package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.PlanoHistorico;

public record PlanoPrecoResponse(Long id, Float valor, LocalDateTime dtModif, boolean renovacao) {

	public static PlanoPrecoResponse from(PlanoHistorico historico) {
		return new PlanoPrecoResponse(historico.getId(), historico.getValor(), historico.getDtModif(),
				historico.isRenovacao());
	}

}
