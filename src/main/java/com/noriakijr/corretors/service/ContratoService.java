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
import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.PagamentoRepository;
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

	public ContratoService(
			ContratoRepository contratos,
			TitularRepository titulares,
			VendedorRepository vendedores,
			PlanoRepository planos,
			PlanoHistoricoRepository historicos,
			PagamentoRepository pagamentos) {
		this.contratos = contratos;
		this.titulares = titulares;
		this.vendedores = vendedores;
		this.planos = planos;
		this.historicos = historicos;
		this.pagamentos = pagamentos;
	}

	@Transactional(readOnly = true)
	public List<ContratoResponse> listByTitular(Long titularId) {
		requireTitular(titularId);
		return contratos.findByTitularIdOrderByDtAquisicaoDesc(titularId).stream()
				.map(ContratoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ContratoResponse get(Long id) {
		return ContratoResponse.from(requireContrato(id));
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

		PlanoHistorico historico = snapshot(plano, request.valor(), agora);
		Pagamento pagamento = createPagamento(
				request.pagamentoDescricao() != null ? request.pagamentoDescricao() : "Aquisicao",
				request.tipoPag() != null ? request.tipoPag() : PagamentoType.DINHEIRO,
				request.valor().doubleValue(),
				request.vlrDesconto());

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
		return ContratoResponse.from(contratos.save(contrato));
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
		LocalDateTime dtInicio = body.dtInicio() != null
				? body.dtInicio()
				: (anterior.getDtFim() != null ? anterior.getDtFim() : agora);
		LocalDateTime dtFim = body.dtFim() != null ? body.dtFim() : dtInicio.plusYears(1);

		PlanoHistorico historico = snapshot(anterior.getPlanoHistorico().getPlano(), valor, agora);
		Pagamento pagamento = createPagamento("Renovacao", PagamentoType.DINHEIRO, valor, 0.0);

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

		anterior.setStatus(ContratoStatus.RENOVADO);
		contratos.save(anterior);
		return ContratoResponse.from(contratos.save(renovado));
	}

	private PlanoHistorico snapshot(Plano plano, float valor, LocalDateTime quando) {
		PlanoHistorico historico = new PlanoHistorico();
		historico.setPlano(plano);
		historico.setValor(valor);
		historico.setDtModif(quando);
		return historicos.save(historico);
	}

	private Pagamento createPagamento(String descricao, PagamentoType tipo, double valor, Double desconto) {
		double vlrDesconto = desconto != null ? desconto : 0.0;
		Pagamento pagamento = new Pagamento();
		pagamento.setDescricao(descricao);
		pagamento.setTipoPag(tipo);
		pagamento.setVlrBruto(valor);
		pagamento.setVlrDesconto(vlrDesconto);
		pagamento.setVlrLiquido(valor - vlrDesconto);
		return pagamentos.save(pagamento);
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
