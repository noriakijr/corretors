package com.noriakijr.corretors.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.service.ContratoService;
import com.noriakijr.corretors.web.dto.ContratoRequest;
import com.noriakijr.corretors.web.dto.ContratoResponse;
import com.noriakijr.corretors.web.dto.RenovarContratoRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ContratoController {

	private final ContratoService contratos;

	public ContratoController(ContratoService contratos) {
		this.contratos = contratos;
	}

	@GetMapping("/titulares/{titularId}/contratos")
	public List<ContratoResponse> listByTitular(@PathVariable Long titularId) {
		return contratos.listByTitular(titularId);
	}

	@GetMapping("/contratos/{id}")
	public ContratoResponse get(@PathVariable Long id) {
		return contratos.get(id);
	}

	@PostMapping("/contratos")
	@ResponseStatus(HttpStatus.CREATED)
	public ContratoResponse create(@Valid @RequestBody ContratoRequest request) {
		return contratos.create(request);
	}

	@PostMapping("/contratos/{id}/renovar")
	@ResponseStatus(HttpStatus.CREATED)
	public ContratoResponse renew(
			@PathVariable Long id,
			@RequestBody(required = false) RenovarContratoRequest request) {
		return contratos.renew(id, request);
	}

}
