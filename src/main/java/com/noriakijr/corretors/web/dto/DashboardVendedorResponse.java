package com.noriakijr.corretors.web.dto;

public record DashboardVendedorResponse(
		Long vendedorId,
		String vendedorNome,
		double valorComissao,
		double receitaCorretora) {
}
