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
import com.noriakijr.corretors.repository.PlanoHistoricoRepository;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.web.dto.PlanoPrecoRequest;
import com.noriakijr.corretors.web.dto.PlanoPrecoResponse;
import com.noriakijr.corretors.web.dto.PlanoRequest;
import com.noriakijr.corretors.web.dto.PlanoResponse;

@Service
public class PlanoService {

	private final PlanoRepository planos;
	private final PlanoHistoricoRepository historicos;

	public PlanoService(PlanoRepository planos, PlanoHistoricoRepository historicos) {
		this.planos = planos;
		this.historicos = historicos;
	}

	@Transactional(readOnly = true)
	public List<PlanoResponse> list() {
		Map<Long, Float> atuais = currentPrices(false);
		Map<Long, Float> renovacoes = currentPrices(true);
		return planos.findAll().stream()
				.map(plano -> PlanoResponse.from(plano, atuais.get(plano.getId()), renovacoes.get(plano.getId())))
				.toList();
	}

	@Transactional
	public PlanoResponse create(PlanoRequest request) {
		Plano plano = new Plano();
		plano.setNome(request.nome().trim());
		plano.setDescricao(request.descricao());
		plano = planos.save(plano);
		Float valor = null;
		Float valorRenovacao = null;
		if (request.valor() != null) {
			valor = saveCatalogPrice(plano, request.valor(), false).getValor();
		}
		if (request.valorRenovacao() != null) {
			valorRenovacao = saveCatalogPrice(plano, request.valorRenovacao(), true).getValor();
		}
		return PlanoResponse.from(plano, valor, valorRenovacao);
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

	private Plano requirePlano(Long id) {
		return planos.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plano nao encontrado"));
	}

}
