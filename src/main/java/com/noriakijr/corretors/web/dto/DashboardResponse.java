package com.noriakijr.corretors.web.dto;

import java.util.List;

public record DashboardResponse(
		long contratosAtivos,
		long contratosVencendo,
		long contratosVencidos,
		long vendasHoje,
		long renovacoesHoje,
		long parcelasAbertas,
		long parcelasVencidas,
		double comissaoMes,
		double receitaCorretoraMes,
		List<DashboardContratoResponse> vencendo,
		List<DashboardContratoResponse> vencidos,
		List<DashboardContratoResponse> vendas,
		List<DashboardContratoResponse> renovacoes,
		List<DashboardParcelaResponse> parcelasVencidasLista,
		List<DashboardVendedorResponse> vendedores) {
}
