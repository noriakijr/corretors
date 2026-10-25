package com.noriakijr.corretors.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.model.Plano;
import com.noriakijr.corretors.repository.PlanoRepository;
import com.noriakijr.corretors.web.dto.PlanoRequest;
import com.noriakijr.corretors.web.dto.PlanoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

	private final PlanoRepository planos;

	public PlanoController(PlanoRepository planos) {
		this.planos = planos;
	}

	@GetMapping
	public List<PlanoResponse> list() {
		return planos.findAll().stream().map(PlanoResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PlanoResponse create(@Valid @RequestBody PlanoRequest request) {
		Plano plano = new Plano();
		plano.setNome(request.nome().trim());
		plano.setDescricao(request.descricao());
		return PlanoResponse.from(planos.save(plano));
	}

}
