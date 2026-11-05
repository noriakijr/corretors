package com.noriakijr.corretors.web.dto;

import java.time.LocalDate;
import java.util.List;

public record FinanceiroResponse(
		LocalDate de,
		LocalDate ate,
		double valorFaturado,
		double comissao,
		double receitaCorretora,
		List<FinanceiroContratoResponse> contratos,
		List<FinanceiroParcelaResponse> parcelas) {
}
