package com.noriakijr.corretors.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.model.Vendedor;
import com.noriakijr.corretors.repository.VendedorRepository;
import com.noriakijr.corretors.web.dto.VendedorRequest;
import com.noriakijr.corretors.web.dto.VendedorResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vendedores")
public class VendedorController {

	private final VendedorRepository vendedores;

	public VendedorController(VendedorRepository vendedores) {
		this.vendedores = vendedores;
	}

	@GetMapping
	public List<VendedorResponse> list() {
		return vendedores.findAll().stream().map(VendedorResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VendedorResponse create(@Valid @RequestBody VendedorRequest request) {
		Vendedor vendedor = new Vendedor();
		vendedor.setNome(request.nome().trim());
		return VendedorResponse.from(vendedores.save(vendedor));
	}

}
