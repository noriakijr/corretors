package com.noriakijr.corretors.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.model.PlanoHistorico;
import com.noriakijr.corretors.model.Seguradora;
import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.PlanoHistoricoRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.repository.SeguradoraRepository;
import com.noriakijr.corretors.repository.VendedorRepository;
import com.noriakijr.corretors.web.dto.ComissaoRequest;
import com.noriakijr.corretors.web.dto.ComissaoVendedorResponse;
import com.noriakijr.corretors.web.dto.PlanoPrecoRequest;
import com.noriakijr.corretors.web.dto.PlanoPrecoResponse;
import com.noriakijr.corretors.web.dto.PlanoRequest;
import com.noriakijr.corretors.web.dto.PlanoResponse;

@Service
public class PlanoService {

	private final PlanoRepository planos;
	private final PlanoHistoricoRepository historicos;
	private final SeguradoraRepository seguradoras;
	private final VendedorRepository vendedores;
	private final ComissaoService comissoes;

	public PlanoService(
			PlanoRepository planos,
			PlanoHistoricoRepository historicos,
			SeguradoraRepository seguradoras,
			VendedorRepository vendedores,
			ComissaoService comissoes) {
		this.planos = planos;
		this.historicos = historicos;
		this.seguradoras = seguradoras;
		this.vendedores = vendedores;
		this.comissoes = comissoes;
	}

	@Transactional(readOnly = true)
	public List<PlanoResponse> list() {
		Map<Long, Float> atuais = currentPrices(false);
		Map<Long, Float> renovacoes = currentPrices(true);
		Map<Long, List<ComissaoVendedorResponse>> excecoes = comissoes.grouped();
		return planos.findAll().stream()
				.map(plano -> PlanoResponse.from(
						plano,
						atuais.get(plano.getId()),
						renovacoes.get(plano.getId()),
						excecoes.getOrDefault(plano.getId(), List.of())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PlanoResponse> listBySeguradora(Long seguradoraId) {
		requireSeguradora(seguradoraId);
		return list().stream().filter(plano -> seguradoraId.equals(plano.seguradoraId())).toList();
	}

	@Transactional
	public PlanoResponse create(Long seguradoraId, PlanoRequest request) {
		Seguradora seguradora = requireSeguradora(seguradoraId);
		Plano plano = new Plano();
		plano.setNome(request.nome().trim());
		plano.setDescricao(request.descricao());
		plano.setLimiteDependentes(request.limiteDependentes());
		plano.setSeguradora(seguradora);
		comissoes.applyPadrao(plano, request.tipoComissao(), request.percentual(), request.valorFixo());
		plano = planos.save(plano);
		Float valor = null;
		Float valorRenovacao = null;
		if (request.valor() != null) {
			valor = saveCatalogPrice(plano, request.valor(), false).getValor();
		}
		if (request.valorRenovacao() != null) {
			valorRenovacao = saveCatalogPrice(plano, request.valorRenovacao(), true).getValor();
		}
		return toResponse(plano, valor, valorRenovacao);
	}

	@Transactional
	public PlanoResponse updateLimite(Long planoId, Integer limiteDependentes) {
		Plano plano = requirePlano(planoId);
		plano.setLimiteDependentes(limiteDependentes);
		plano = planos.save(plano);
		return toResponse(plano);
	}

	@Transactional
	public PlanoResponse saveComissao(Long planoId, ComissaoRequest request) {
		Plano plano = requirePlano(planoId);
		comissoes.applyPadrao(plano, request.tipoComissao(), request.percentual(), request.valorFixo());
		plano = planos.save(plano);
		return toResponse(plano);
	}

	@Transactional
	public PlanoResponse saveExcecao(Long planoId, Long vendedorId, ComissaoRequest request) {
		Plano plano = requirePlano(planoId);
		Vendedor vendedor = vendedores.findById(vendedorId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "vendedor nao encontrado"));
		comissoes.upsert(plano, vendedor, request.tipoComissao(), request.percentual(), request.valorFixo());
		return toResponse(plano);
	}

	@Transactional
	public PlanoResponse deleteExcecao(Long planoId, Long vendedorId) {
		Plano plano = requirePlano(planoId);
		comissoes.remove(plano.getId(), vendedorId);
		return toResponse(plano);
	}

	@Transactional(readOnly = true)
	public List<PlanoPrecoResponse> history(Long planoId) {
		requirePlano(planoId);
		return historicos.findByPlanoIdAndCatalogoTrueOrderByDtModifDescIdDesc(planoId).stream()
				.map(PlanoPrecoResponse::from)
				.toList();
	}

	@Transactional
	public PlanoPrecoResponse addPrice(Long planoId, PlanoPrecoRequest request) {
		Plano plano = requirePlano(planoId);
		boolean renovacao = Boolean.TRUE.equals(request.renovacao());
		return PlanoPrecoResponse.from(saveCatalogPrice(plano, request.valor(), renovacao));
	}

	private PlanoHistorico saveCatalogPrice(Plano plano, float valor, boolean renovacao) {
		PlanoHistorico historico = new PlanoHistorico();
		historico.setPlano(plano);
		historico.setValor(valor);
		historico.setDtModif(LocalDateTime.now());
		historico.setCatalogo(true);
		historico.setRenovacao(renovacao);
		return historicos.save(historico);
	}

	private Map<Long, Float> currentPrices(boolean renovacao) {
		Map<Long, Float> atuais = new HashMap<>();
		for (PlanoHistorico historico : historicos.findByCatalogoTrueOrderByDtModifDescIdDesc()) {
			if (historico.isRenovacao() != renovacao) {
				continue;
			}
			if (historico.getPlano() == null || historico.getPlano().getId() == null) {
				continue;
			}
			atuais.putIfAbsent(historico.getPlano().getId(), historico.getValor());
		}
		return atuais;
	}

	private PlanoResponse toResponse(Plano plano) {
		Map<Long, Float> atuais = currentPrices(false);
		Map<Long, Float> renovacoes = currentPrices(true);
		return toResponse(plano, atuais.get(plano.getId()), renovacoes.get(plano.getId()));
	}

	private PlanoResponse toResponse(Plano plano, Float valor, Float valorRenovacao) {
		return PlanoResponse.from(plano, valor, valorRenovacao, comissoes.list(plano.getId()));
	}

	private Plano requirePlano(Long id) {
		return planos.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plano nao encontrado"));
	}

	private Seguradora requireSeguradora(Long id) {
		return seguradoras.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "seguradora nao encontrada"));
	}

}
