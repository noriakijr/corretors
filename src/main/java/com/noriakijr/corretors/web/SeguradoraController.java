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
import org.springframework.web.server.ResponseStatusException;

import com.noriakijr.corretors.model.Seguradora;
import com.noriakijr.corretors.repository.SeguradoraRepository;
import com.noriakijr.corretors.service.PlanoService;
import com.noriakijr.corretors.web.dto.PlanoRequest;
import com.noriakijr.corretors.web.dto.PlanoResponse;
import com.noriakijr.corretors.web.dto.SeguradoraRequest;
import com.noriakijr.corretors.web.dto.SeguradoraResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seguradoras")
public class SeguradoraController {

	private final SeguradoraRepository seguradoras;
	private final PlanoService planos;

	public SeguradoraController(SeguradoraRepository seguradoras, PlanoService planos) {
		this.seguradoras = seguradoras;
		this.planos = planos;
	}

	@GetMapping
	public List<SeguradoraResponse> list() {
		return seguradoras.findAll().stream().map(SeguradoraResponse::from).toList();
	}

	@GetMapping("/{id}")
	public SeguradoraResponse get(@PathVariable Long id) {
		return SeguradoraResponse.from(require(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SeguradoraResponse create(@Valid @RequestBody SeguradoraRequest request) {
		Seguradora seguradora = new Seguradora();
		seguradora.setNome(request.nome().trim());
		return SeguradoraResponse.from(seguradoras.save(seguradora));
	}

	@GetMapping("/{id}/planos")
	public List<PlanoResponse> planos(@PathVariable Long id) {
		return planos.listBySeguradora(id);
	}

	@PostMapping("/{id}/planos")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoResponse createPlano(@PathVariable Long id, @Valid @RequestBody PlanoRequest request) {
		return planos.create(id, request);
	}

	private Seguradora require(Long id) {
		return seguradoras.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "seguradora nao encontrada"));
	}

}
