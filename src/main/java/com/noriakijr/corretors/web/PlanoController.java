package com.noriakijr.corretors.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.service.PlanoService;
import com.noriakijr.corretors.web.dto.ComissaoRequest;
import com.noriakijr.corretors.web.dto.PlanoLimiteRequest;
import com.noriakijr.corretors.web.dto.PlanoPrecoRequest;
import com.noriakijr.corretors.web.dto.PlanoPrecoResponse;
import com.noriakijr.corretors.web.dto.PlanoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

	private final PlanoService planos;

	public PlanoController(PlanoService planos) {
		this.planos = planos;
	}

	@GetMapping
	public List<PlanoResponse> list() {
		return planos.list();
	}

	@PutMapping("/{id}")
	public PlanoResponse updateLimite(@PathVariable Long id, @Valid @RequestBody PlanoLimiteRequest request) {
		return planos.updateLimite(id, request.limiteDependentes());
	}

	@PutMapping("/{id}/comissao")
	public PlanoResponse saveComissao(@PathVariable Long id, @Valid @RequestBody ComissaoRequest request) {
		return planos.saveComissao(id, request);
	}

	@PutMapping("/{id}/comissoes/{vendedorId}")
	public PlanoResponse saveExcecao(
			@PathVariable Long id,
			@PathVariable Long vendedorId,
			@Valid @RequestBody ComissaoRequest request) {
		return planos.saveExcecao(id, vendedorId, request);
	}

	@DeleteMapping("/{id}/comissoes/{vendedorId}")
	public PlanoResponse deleteExcecao(@PathVariable Long id, @PathVariable Long vendedorId) {
		return planos.deleteExcecao(id, vendedorId);
	}

	@GetMapping("/{id}/precos")
	public List<PlanoPrecoResponse> precos(@PathVariable Long id) {
		return planos.history(id);
	}

	@PostMapping("/{id}/precos")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoPrecoResponse addPreco(@PathVariable Long id, @Valid @RequestBody PlanoPrecoRequest request) {
		return planos.addPrice(id, request);
	}

}
