package com.noriakijr.corretors.web.dto;

import java.time.LocalDateTime;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.Seguradora;

public record FinanceiroContratoResponse(
		Long id,
		String codigo,
		Long titularId,
		String titularNome,
		Long seguradoraId,
		String seguradoraNome,
		Long vendedorId,
		String vendedorNome,
		LocalDateTime dtAquisicao,
		Double total,
		Double desconto,
		Double valorComissao,
		Double receitaCorretora) {

	public static FinanceiroContratoResponse from(Contrato contrato) {
		Pagamento pagamento = contrato.getPagamento();
		Seguradora seguradora = seguradora(contrato);
		return new FinanceiroContratoResponse(
				contrato.getId(),
				contrato.getCodigo(),
				contrato.getTitular() != null ? contrato.getTitular().getId() : null,
				contrato.getTitular() != null ? contrato.getTitular().getNome() : null,
				seguradora != null ? seguradora.getId() : null,
				seguradora != null ? seguradora.getNome() : null,
				contrato.getVendedor() != null ? contrato.getVendedor().getId() : null,
				contrato.getVendedor() != null ? contrato.getVendedor().getNome() : null,
				contrato.getDtAquisicao(),
				total(contrato),
				desconto(pagamento),
				contrato.getValorComissao(),
				contrato.getReceitaCorretora());
	}

	private static Seguradora seguradora(Contrato contrato) {
		if (contrato.getPlanoHistorico() == null || contrato.getPlanoHistorico().getPlano() == null) {
			return null;
		}
		return contrato.getPlanoHistorico().getPlano().getSeguradora();
	}

	private static Double total(Contrato contrato) {
		Pagamento pagamento = contrato.getPagamento();
		if (pagamento != null && pagamento.getVlrBruto() != null) {
			return pagamento.getVlrBruto();
		}
		if (contrato.getPlanoHistorico() != null && contrato.getPlanoHistorico().getValor() != null) {
			return contrato.getPlanoHistorico().getValor().doubleValue();
		}
		return null;
	}

	private static Double desconto(Pagamento pagamento) {
		if (pagamento != null && pagamento.getVlrDesconto() != null) {
			return pagamento.getVlrDesconto();
		}
		return 0d;
	}
}
