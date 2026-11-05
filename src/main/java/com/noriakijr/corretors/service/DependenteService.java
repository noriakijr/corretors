package com.noriakijr.corretors.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.Contrato;
import com.noriakijr.corretors.model.ContratoStatus;
import com.noriakijr.corretors.model.Dependente;
import com.noriakijr.corretors.repository.ContratoRepository;
import com.noriakijr.corretors.repository.DependenteRepository;
import com.noriakijr.corretors.web.dto.DependenteRequest;
import com.noriakijr.corretors.web.dto.DependenteResponse;

@Service
public class DependenteService {

	static final String NAO_VIGENTE = "contrato nao esta vigente";

	private final DependenteRepository dependentes;
	private final ContratoRepository contratos;

	public DependenteService(DependenteRepository dependentes, ContratoRepository contratos) {
		this.dependentes = dependentes;
		this.contratos = contratos;
	}

	@Transactional(readOnly = true)
	public List<DependenteResponse> list(Long contratoId) {
		requireContrato(contratoId);
		return dependentes.findByContratoIdOrderByIdAsc(contratoId).stream()
				.map(DependenteResponse::from)
				.toList();
	}

	@Transactional
	public DependenteResponse create(Long contratoId, DependenteRequest request) {
		Contrato contrato = requireContrato(contratoId);
		assertVigente(contrato);
		return DependenteResponse.from(saveNew(contrato, request));
	}

	@Transactional
	public DependenteResponse update(Long contratoId, Long dependenteId, DependenteRequest request) {
		Contrato contrato = requireContrato(contratoId);
		assertVigente(contrato);
		Dependente dependente = requireDependente(contratoId, dependenteId);
		apply(dependente, request);
		return DependenteResponse.from(dependentes.save(dependente));
	}

	@Transactional
	public void delete(Long contratoId, Long dependenteId) {
		Contrato contrato = requireContrato(contratoId);
		assertVigente(contrato);
		Dependente dependente = requireDependente(contratoId, dependenteId);
		dependentes.delete(dependente);
	}

	@Transactional
	public void attach(Contrato contrato, List<DependenteRequest> itens) {
		if (itens == null || itens.isEmpty()) {
			return;
		}
		assertVigente(contrato);
		for (DependenteRequest item : itens) {
			saveNew(contrato, item);
		}
	}

	@Transactional
	public void copy(Contrato origem, Contrato destino) {
		for (Dependente item : dependentes.findByContratoIdOrderByIdAsc(origem.getId())) {
			Dependente copia = new Dependente();
			copia.setNome(item.getNome());
			copia.setParentesco(item.getParentesco());
			copia.setDtNasc(item.getDtNasc());
			copia.setContrato(destino);
			copia.setTitular(destino.getTitular());
			dependentes.save(copia);
		}
	}

	@Transactional(readOnly = true)
	public boolean pontoAtencao(Contrato contrato) {
		return acimaDoLimite(dependentes.countByContratoId(contrato.getId()), limite(contrato));
	}

	static boolean acimaDoLimite(long quantidade, Integer limite) {
		return limite != null && quantidade > limite;
	}

	private Dependente saveNew(Contrato contrato, DependenteRequest request) {
		Dependente dependente = new Dependente();
		apply(dependente, request);
		dependente.setContrato(contrato);
		dependente.setTitular(contrato.getTitular());
		return dependentes.save(dependente);
	}

	private static void apply(Dependente dependente, DependenteRequest request) {
		dependente.setNome(request.nome().trim());
		dependente.setParentesco(request.parentesco().trim());
		dependente.setDtNasc(request.dtNasc());
	}

	static void assertVigente(Contrato contrato) {
		if (!vigente(contrato, LocalDate.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NAO_VIGENTE);
		}
	}

	static boolean vigente(Contrato contrato, LocalDate hoje) {
		if (contrato.getStatus() != ContratoStatus.ATIVO) {
			return false;
		}
		LocalDateTime inicio = contrato.getDtInicio();
		LocalDateTime fim = contrato.getDtFim();
		if (inicio == null || fim == null) {
			return false;
		}
		LocalDate dataInicio = inicio.toLocalDate();
		LocalDate dataFim = fim.toLocalDate();
		return !hoje.isBefore(dataInicio) && !hoje.isAfter(dataFim);
	}

	private static Integer limite(Contrato contrato) {
		if (contrato.getPlanoHistorico() == null || contrato.getPlanoHistorico().getPlano() == null) {
			return null;
		}
		return contrato.getPlanoHistorico().getPlano().getLimiteDependentes();
	}

	private Contrato requireContrato(Long id) {
		return contratos.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "contrato nao encontrado"));
	}

	private Dependente requireDependente(Long contratoId, Long dependenteId) {
		return dependentes.findByIdAndContratoId(dependenteId, contratoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dependente nao encontrado"));
	}

}
