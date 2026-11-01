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

import com.noriakijr.corretors.service.PlanoService;
import com.noriakijr.corretors.web.dto.PlanoPrecoRequest;
import com.noriakijr.corretors.web.dto.PlanoPrecoResponse;
import com.noriakijr.corretors.web.dto.PlanoRequest;
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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoResponse create(@Valid @RequestBody PlanoRequest request) {
		return planos.create(request);
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
