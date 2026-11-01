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

import com.noriakijr.corretors.service.DependenteService;
import com.noriakijr.corretors.web.dto.DependenteRequest;
import com.noriakijr.corretors.web.dto.DependenteResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contratos/{contratoId}/dependentes")
public class DependenteController {

	private final DependenteService dependentes;

	public DependenteController(DependenteService dependentes) {
		this.dependentes = dependentes;
	}

	@GetMapping
	public List<DependenteResponse> list(@PathVariable Long contratoId) {
		return dependentes.list(contratoId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DependenteResponse create(
			@PathVariable Long contratoId,
			@Valid @RequestBody DependenteRequest request) {
		return dependentes.create(contratoId, request);
	}

	@PutMapping("/{id}")
	public DependenteResponse update(
			@PathVariable Long contratoId,
			@PathVariable Long id,
			@Valid @RequestBody DependenteRequest request) {
		return dependentes.update(contratoId, id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long contratoId, @PathVariable Long id) {
		dependentes.delete(contratoId, id);
	}

}
