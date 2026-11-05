package com.noriakijr.corretors.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.ParcelaStatus;
import com.noriakijr.corretors.model.Seguradora;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.ParcelaRepository;
import com.noriakijr.corretors.web.dto.DashboardContratoResponse;
import com.noriakijr.corretors.web.dto.DashboardParcelaResponse;
import com.noriakijr.corretors.web.dto.DashboardResponse;
import com.noriakijr.corretors.web.dto.DashboardVendedorResponse;
import com.noriakijr.corretors.web.dto.FinanceiroContratoResponse;
import com.noriakijr.corretors.web.dto.FinanceiroParcelaResponse;
import com.noriakijr.corretors.web.dto.FinanceiroResponse;

@Service
public class FinanceiroService {

	private final ContratoRepository contratos;
	private final ParcelaRepository parcelas;

	public FinanceiroService(ContratoRepository contratos, ParcelaRepository parcelas) {
		this.contratos = contratos;
		this.parcelas = parcelas;
	}

	@Transactional(readOnly = true)
	public DashboardResponse dashboard() {
		LocalDate hoje = LocalDate.now();
		YearMonth mes = YearMonth.from(hoje);
		List<Contrato> todos = contratos.findAllForRelatorio();
		Map<Long, Contrato> porPagamento = porPagamento(todos);

		List<Contrato> ativos = new ArrayList<>();
		List<Contrato> vencendo = new ArrayList<>();
		List<Contrato> vencidos = new ArrayList<>();
		List<Contrato> vendas = new ArrayList<>();
		List<Contrato> renovacoes = new ArrayList<>();
		List<Contrato> doMes = new ArrayList<>();
		for (Contrato contrato : todos) {
			if (contrato.getStatus() == ContratoStatus.ATIVO) {
				ativos.add(contrato);
				LocalDate fim = data(contrato.getDtFim());
				if (fim != null && !fim.isBefore(hoje) && !fim.isAfter(hoje.plusDays(30))) {
					vencendo.add(contrato);
				}
				if (fim != null && fim.isBefore(hoje)) {
					vencidos.add(contrato);
				}
			}
			LocalDate aquisicao = data(contrato.getDtAquisicao());
			if (aquisicao != null && aquisicao.equals(hoje)) {
				if (contrato.getContratoAnterior() == null) {
					vendas.add(contrato);
				} else {
					renovacoes.add(contrato);
				}
			}
			if (aquisicao != null && YearMonth.from(aquisicao).equals(mes)) {
				doMes.add(contrato);
			}
		}

		long abertas = 0;
		long vencidas = 0;
		List<DashboardParcelaResponse> parcelasVencidas = new ArrayList<>();
		for (Parcela parcela : parcelas.findAllWithPagamento()) {
			ParcelaStatus status = ParcelaStatus.of(parcela.getDtParcela(), parcela.getDtPagamento(), hoje);
			if (status == ParcelaStatus.ABERTA) {
				abertas++;
			} else if (status == ParcelaStatus.VENCIDA) {
				vencidas++;
				Contrato contrato = contratoDa(parcela, porPagamento);
				parcelasVencidas.add(DashboardParcelaResponse.from(parcela, contrato, status));
			}
		}

		vencendo.sort(Comparator.comparing(Contrato::getDtFim, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Contrato::getId, Comparator.nullsLast(Comparator.naturalOrder())));
		vencidos.sort(Comparator.comparing(Contrato::getDtFim, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Contrato::getId, Comparator.nullsLast(Comparator.naturalOrder())));
		vendas.sort(porAquisicaoDesc());
		renovacoes.sort(porAquisicaoDesc());
		parcelasVencidas.sort(Comparator.comparing(
				DashboardParcelaResponse::dtParcela,
				Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(DashboardParcelaResponse::id, Comparator.nullsLast(Comparator.naturalOrder())));

		long comissao = 0;
		long receita = 0;
		for (Contrato contrato : doMes) {
			comissao += cents(contrato.getValorComissao());
			receita += cents(contrato.getReceitaCorretora());
		}

		return new DashboardResponse(
				ativos.size(),
				vencendo.size(),
				vencidos.size(),
				vendas.size(),
				renovacoes.size(),
				abertas,
				vencidas,
				money(comissao),
				money(receita),
				vencendo.stream().map(DashboardContratoResponse::from).toList(),
				vencidos.stream().map(DashboardContratoResponse::from).toList(),
				vendas.stream().map(DashboardContratoResponse::from).toList(),
				renovacoes.stream().map(DashboardContratoResponse::from).toList(),
				List.copyOf(parcelasVencidas),
				vendedoresDoMes(doMes));
	}

	@Transactional(readOnly = true)
	public FinanceiroResponse consultar(LocalDate de, LocalDate ate, Long seguradoraId, Long vendedorId) {
		LocalDate hoje = LocalDate.now();
		LocalDate inicio = de != null ? de : YearMonth.from(hoje).atDay(1);
		LocalDate fim = ate != null ? ate : YearMonth.from(hoje).atEndOfMonth();
		if (inicio.isAfter(fim)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodo invalido");
		}

		List<Contrato> todos = contratos.findAllForRelatorio();
		Map<Long, Contrato> porPagamento = porPagamento(todos);
		List<Contrato> doPeriodo = todos.stream()
				.filter(contrato -> noPeriodo(data(contrato.getDtAquisicao()), inicio, fim))
				.filter(contrato -> mesmaSeguradora(contrato, seguradoraId))
				.filter(contrato -> mesmoVendedor(contrato, vendedorId))
				.sorted(porAquisicaoDesc())
				.toList();

		long faturado = 0;
		long comissao = 0;
		long receita = 0;
		for (Contrato contrato : doPeriodo) {
			faturado += cents(liquido(contrato));
			comissao += cents(contrato.getValorComissao());
			receita += cents(contrato.getReceitaCorretora());
		}

		List<FinanceiroParcelaResponse> parcelasPeriodo = new ArrayList<>();
		for (Parcela parcela : parcelas.findAllWithPagamento()) {
			if (!noPeriodo(data(parcela.getDtParcela()), inicio, fim)) {
				continue;
			}
			Contrato contrato = contratoDa(parcela, porPagamento);
			if (contrato == null) {
				continue;
			}
			if (!mesmaSeguradora(contrato, seguradoraId) || !mesmoVendedor(contrato, vendedorId)) {
				continue;
			}
			parcelasPeriodo.add(FinanceiroParcelaResponse.from(parcela, contrato, hoje));
		}
		parcelasPeriodo.sort(Comparator
				.comparing(FinanceiroParcelaResponse::dtParcela, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(FinanceiroParcelaResponse::id, Comparator.nullsLast(Comparator.naturalOrder())));

		return new FinanceiroResponse(
				inicio,
				fim,
				money(faturado),
				money(comissao),
				money(receita),
				doPeriodo.stream().map(FinanceiroContratoResponse::from).toList(),
				List.copyOf(parcelasPeriodo));
	}

	@Transactional
	public FinanceiroParcelaResponse pagar(Long id) {
		Parcela parcela = parcelas.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parcela nao encontrada"));
		if (parcela.getDtPagamento() == null) {
			parcela.setDtPagamento(LocalDate.now().atStartOfDay());
			parcela = parcelas.save(parcela);
		}
		Contrato contrato = null;
		if (parcela.getPagamento() != null && parcela.getPagamento().getId() != null) {
			contrato = contratos.findByPagamentoId(parcela.getPagamento().getId()).orElse(null);
		}
		return FinanceiroParcelaResponse.from(parcela, contrato, LocalDate.now());
	}

	private List<DashboardVendedorResponse> vendedoresDoMes(List<Contrato> doMes) {
		Map<Long, long[]> totais = new HashMap<>();
		Map<Long, String> nomes = new HashMap<>();
		for (Contrato contrato : doMes) {
			long chave = contrato.getVendedor() != null && contrato.getVendedor().getId() != null
					? contrato.getVendedor().getId()
					: -1L;
			long[] acc = totais.computeIfAbsent(chave, ignorado -> new long[2]);
			acc[0] += cents(contrato.getValorComissao());
			acc[1] += cents(contrato.getReceitaCorretora());
			nomes.putIfAbsent(chave, contrato.getVendedor() != null ? contrato.getVendedor().getNome() : null);
		}
		return totais.entrySet().stream()
				.map(entry -> new DashboardVendedorResponse(
						entry.getKey() == -1L ? null : entry.getKey(),
						nomes.get(entry.getKey()),
						money(entry.getValue()[0]),
						money(entry.getValue()[1])))
				.sorted(Comparator.comparing(
						item -> item.vendedorNome() == null ? "" : item.vendedorNome(),
						String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private static Map<Long, Contrato> porPagamento(List<Contrato> todos) {
		Map<Long, Contrato> porPagamento = new HashMap<>();
		for (Contrato contrato : todos) {
			if (contrato.getPagamento() != null && contrato.getPagamento().getId() != null) {
				porPagamento.putIfAbsent(contrato.getPagamento().getId(), contrato);
			}
		}
		return porPagamento;
	}

	private static Contrato contratoDa(Parcela parcela, Map<Long, Contrato> porPagamento) {
		if (parcela.getPagamento() == null || parcela.getPagamento().getId() == null) {
			return null;
		}
		return porPagamento.get(parcela.getPagamento().getId());
	}

	private static boolean noPeriodo(LocalDate data, LocalDate inicio, LocalDate fim) {
		return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
	}

	private static boolean mesmaSeguradora(Contrato contrato, Long seguradoraId) {
		if (seguradoraId == null) {
			return true;
		}
		Seguradora seguradora = seguradora(contrato);
		return seguradora != null && seguradoraId.equals(seguradora.getId());
	}

	private static boolean mesmoVendedor(Contrato contrato, Long vendedorId) {
		if (vendedorId == null) {
			return true;
		}
		return contrato.getVendedor() != null && vendedorId.equals(contrato.getVendedor().getId());
	}

	private static Seguradora seguradora(Contrato contrato) {
		if (contrato.getPlanoHistorico() == null || contrato.getPlanoHistorico().getPlano() == null) {
			return null;
		}
		return contrato.getPlanoHistorico().getPlano().getSeguradora();
	}

	private static Double liquido(Contrato contrato) {
		Pagamento pagamento = contrato.getPagamento();
		if (pagamento != null && pagamento.getVlrLiquido() != null) {
			return pagamento.getVlrLiquido();
		}
		if (pagamento != null && pagamento.getVlrBruto() != null) {
			long bruto = cents(pagamento.getVlrBruto());
			long desconto = cents(pagamento.getVlrDesconto());
			return (bruto - desconto) / 100.0;
		}
		if (contrato.getPlanoHistorico() != null && contrato.getPlanoHistorico().getValor() != null) {
			return contrato.getPlanoHistorico().getValor().doubleValue();
		}
		return 0d;
	}

	private static LocalDate data(LocalDateTime valor) {
		return valor == null ? null : valor.toLocalDate();
	}

	private static Comparator<Contrato> porAquisicaoDesc() {
		return Comparator.comparing(Contrato::getDtAquisicao, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(Contrato::getId, Comparator.nullsLast(Comparator.reverseOrder()));
	}

	private static long cents(Double valor) {
		if (valor == null) {
			return 0;
		}
		return Math.round(valor * 100.0);
	}

	private static double money(long cents) {
		return cents / 100.0;
	}

}
