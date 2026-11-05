package com.noriakijr.corretors.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Pagamento;
import com.noriakijr.corretors.model.PagamentoType;
import com.noriakijr.corretors.model.Parcela;
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.PagamentoRepository;
import com.noriakijr.corretors.repository.ParcelaRepository;
import com.noriakijr.corretors.repository.PlanoHistoricoRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.repository.VendedorRepository;
import com.noriakijr.corretors.web.dto.ContratoRequest;
import com.noriakijr.corretors.web.dto.ContratoResponse;
import com.noriakijr.corretors.web.dto.RenovarContratoRequest;

@Service
public class ContratoService {

	private final ContratoRepository contratos;
	private final TitularRepository titulares;
	private final VendedorRepository vendedores;
	private final PlanoRepository planos;
	private final PlanoHistoricoRepository historicos;
	private final PagamentoRepository pagamentos;
	private final ParcelaRepository parcelas;
	private final DependenteService dependentes;
	private final ComissaoService comissoes;

	public ContratoService(
			ContratoRepository contratos,
			TitularRepository titulares,
			VendedorRepository vendedores,
			PlanoRepository planos,
			PlanoHistoricoRepository historicos,
			PagamentoRepository pagamentos,
			ParcelaRepository parcelas,
			DependenteService dependentes,
			ComissaoService comissoes) {
		this.contratos = contratos;
		this.titulares = titulares;
		this.vendedores = vendedores;
		this.planos = planos;
		this.historicos = historicos;
		this.pagamentos = pagamentos;
		this.parcelas = parcelas;
		this.dependentes = dependentes;
		this.comissoes = comissoes;
	}

	@Transactional(readOnly = true)
	public List<ContratoResponse> listByTitular(Long titularId) {
		requireTitular(titularId);
		return contratos.findByTitularIdOrderByDtAquisicaoDesc(titularId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public ContratoResponse get(Long id) {
		return toResponse(requireContrato(id));
	}

	@Transactional
	public ContratoResponse create(ContratoRequest request) {
		Titular titular = requireTitular(request.titularId());
		Vendedor vendedor = requireVendedor(request.vendedorId());
		Plano plano = requirePlano(request.planoId());

		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime dtAquisicao = request.dtAquisicao() != null ? request.dtAquisicao() : agora;
		LocalDateTime dtInicio = request.dtInicio() != null ? request.dtInicio() : dtAquisicao;
		LocalDateTime dtFim = request.dtFim() != null ? request.dtFim() : dtInicio.plusYears(1);
		double liquido = liquido(request.valor(), request.vlrDesconto());
		int quantidade = quantidade(request.quantidadeParcelas());

		PlanoHistorico historico = snapshot(plano, (float) liquido, agora);
		Pagamento pagamento = createPagamento(
				request.pagamentoDescricao() != null ? request.pagamentoDescricao() : "Aquisicao",
				request.tipoPag() != null ? request.tipoPag() : PagamentoType.DINHEIRO,
				request.valor().doubleValue(),
				request.vlrDesconto() != null ? request.vlrDesconto() : 0.0,
				liquido,
				quantidade);
		if (quantidade > 1) {
			createParcelas(pagamento, quantidade, dtInicio, liquido);
		}

		Contrato contrato = new Contrato();
		contrato.setCodigo(request.codigo());
		contrato.setDescricao(request.descricao());
		contrato.setDtAquisicao(dtAquisicao);
		contrato.setDtInicio(dtInicio);
		contrato.setDtFim(dtFim);
		contrato.setStatus(ContratoStatus.ATIVO);
		contrato.setTitular(titular);
		contrato.setVendedor(vendedor);
		contrato.setPlanoHistorico(historico);
		contrato.setPagamento(pagamento);
		comissoes.aplicar(contrato, plano, vendedor, liquido);
		contrato = contratos.save(contrato);
		dependentes.attach(contrato, request.dependentes());
		return toResponse(contrato);
	}

	@Transactional
	public ContratoResponse renew(Long contratoId, RenovarContratoRequest request) {
		Contrato anterior = requireContrato(contratoId);
		if (anterior.getStatus() != ContratoStatus.ATIVO) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"somente contratos ATIVO podem ser renovados");
		}
		if (anterior.getPlanoHistorico() == null || anterior.getPlanoHistorico().getPlano() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"contrato sem plano para renovar");
		}

		RenovarContratoRequest body = request != null
				? request
				: new RenovarContratoRequest(null, null, null, null, null);
		LocalDateTime agora = LocalDateTime.now();
		float valor = body.valor() != null ? body.valor() : anterior.getPlanoHistorico().getValor();
		if (valor < 0f) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"valor da renovacao nao pode ser negativo");
		}
		LocalDateTime dtInicio = body.dtInicio() != null ? body.dtInicio() : agora;
		LocalDateTime dtFim = body.dtFim() != null ? body.dtFim() : dtInicio.plusYears(1);

		Plano plano = anterior.getPlanoHistorico().getPlano();
		PlanoHistorico historico = snapshot(plano, valor, agora);
		Pagamento pagamento = createPagamento("Renovacao", PagamentoType.DINHEIRO, valor, 0.0, valor, 1);

		Contrato renovado = new Contrato();
		renovado.setCodigo(body.codigo() != null ? body.codigo() : nextCodigo(anterior.getCodigo()));
		renovado.setDescricao(body.descricao() != null ? body.descricao() : anterior.getDescricao());
		renovado.setDtAquisicao(agora);
		renovado.setDtInicio(dtInicio);
		renovado.setDtFim(dtFim);
		renovado.setStatus(ContratoStatus.ATIVO);
		renovado.setTitular(anterior.getTitular());
		renovado.setVendedor(anterior.getVendedor());
		renovado.setPlanoHistorico(historico);
		renovado.setPagamento(pagamento);
		renovado.setContratoAnterior(anterior);
		comissoes.aplicar(renovado, plano, anterior.getVendedor(), valor);

		anterior.setStatus(ContratoStatus.RENOVADO);
		contratos.save(anterior);
		renovado = contratos.save(renovado);
		dependentes.copy(anterior, renovado);
		return toResponse(renovado);
	}

	private ContratoResponse toResponse(Contrato contrato) {
		return ContratoResponse.from(contrato, dependentes.pontoAtencao(contrato));
	}

	private PlanoHistorico snapshot(Plano plano, float valor, LocalDateTime quando) {
		PlanoHistorico historico = new PlanoHistorico();
		historico.setPlano(plano);
		historico.setValor(valor);
		historico.setDtModif(quando);
		historico.setCatalogo(false);
		historico.setRenovacao(false);
		return historicos.save(historico);
	}

	private Pagamento createPagamento(
			String descricao,
			PagamentoType tipo,
			double bruto,
			double desconto,
			double liquido,
			int quantidade) {
		Pagamento pagamento = new Pagamento();
		pagamento.setDescricao(descricao);
		pagamento.setTipoPag(tipo);
		pagamento.setVlrBruto(bruto);
		pagamento.setVlrDesconto(desconto);
		pagamento.setVlrLiquido(liquido);
		pagamento.setQuantidade(quantidade);
		return pagamentos.save(pagamento);
	}

	private void createParcelas(Pagamento pagamento, int quantidade, LocalDateTime inicio, double liquido) {
		long cents = Math.round(liquido * 100.0);
		long base = cents / quantidade;
		long remainder = cents % quantidade;
		for (int index = 0; index < quantidade; index++) {
			long parcelaCents = base + (index == quantidade - 1 ? remainder : 0);
			Parcela parcela = new Parcela();
			parcela.setVlrParcela(parcelaCents / 100.0);
			parcela.setDtParcela(inicio.plusMonths(index));
			parcela.setPagamento(pagamento);
			parcelas.save(parcela);
		}
	}

	private static double liquido(float valor, Double desconto) {
		long brutoCents = Math.round((double) valor * 100.0);
		long descontoCents = desconto == null ? 0 : Math.round(desconto * 100.0);
		if (descontoCents < 0 || descontoCents > brutoCents) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"desconto nao pode ser maior que o valor");
		}
		return (brutoCents - descontoCents) / 100.0;
	}

	private static int quantidade(Integer quantidadeParcelas) {
		if (quantidadeParcelas == null) {
			return 1;
		}
		if (quantidadeParcelas < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantidade de parcelas invalida");
		}
		return quantidadeParcelas;
	}

	private Contrato requireContrato(Long id) {
		return contratos.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "contrato nao encontrado"));
	}

	private Titular requireTitular(Long id) {
		return titulares.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "titular nao encontrado"));
	}

	private Vendedor requireVendedor(Long id) {
		return vendedores.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendedor nao encontrado"));
	}

	private Plano requirePlano(Long id) {
		return planos.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plano nao encontrado"));
	}

	private static String nextCodigo(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			return null;
		}
		return codigo + "-R";
	}

}
