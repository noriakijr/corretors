package com.noriakijr.corretors.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public enum ParcelaStatus {
	ABERTA,
	PAGA,
	VENCIDA;

	public static ParcelaStatus of(LocalDateTime dtParcela, LocalDateTime dtPagamento, LocalDate hoje) {
		if (dtPagamento != null) {
			return PAGA;
		}
		if (dtParcela != null && dtParcela.toLocalDate().isBefore(hoje)) {
			return VENCIDA;
		}
		return ABERTA;
	}
}
