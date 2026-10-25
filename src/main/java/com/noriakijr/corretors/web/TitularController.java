package com.noriakijr.corretors.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.model.Contato;
import com.noriakijr.corretors.model.Endereco;
import com.noriakijr.corretors.model.Titular;
import com.noriakijr.corretors.repository.TitularRepository;
import com.noriakijr.corretors.web.dto.ContatoRequest;
import com.noriakijr.corretors.web.dto.EnderecoRequest;
import com.noriakijr.corretors.web.dto.TitularRequest;
import com.noriakijr.corretors.web.dto.TitularResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/titulares")
public class TitularController {

	private final TitularRepository titulares;

	public TitularController(TitularRepository titulares) {
		this.titulares = titulares;
	}

	@GetMapping
	public List<TitularResponse> list() {
		return titulares.findAll().stream().map(TitularResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TitularResponse create(@Valid @RequestBody TitularRequest request) {
		Titular titular = new Titular();
		titular.setNome(request.nome().trim());
		titular.setCpf(request.cpf());
		titular.setRg(request.rg());
		titular.setDtNasc(request.dtNasc());
		titular.setCartaoEnv(request.cartaoEnv());
		titular.setSeguroVidaEnv(request.seguroVidaEnv());
		titular.setContato(toContato(request.contato()));
		return TitularResponse.from(titulares.save(titular));
	}

	private static Contato toContato(ContatoRequest request) {
		if (request == null) {
			return null;
		}
		Contato contato = new Contato();
		contato.setTelFixo(request.telFixo());
		contato.setTelComercial(request.telComercial());
		contato.setTelFax(request.telFax());
		contato.setCelular(request.celular());
		contato.setEmail(request.email());
		contato.setEndereco(toEndereco(request.endereco()));
		return contato;
	}

	private static Endereco toEndereco(EnderecoRequest request) {
		if (request == null) {
			return null;
		}
		Endereco endereco = new Endereco();
		endereco.setPais(request.pais());
		endereco.setEstado(request.estado());
		endereco.setCidade(request.cidade());
		endereco.setBairro(request.bairro());
		endereco.setRua(request.rua());
		endereco.setNumero(request.numero());
		endereco.setLatitude(request.latitude());
		endereco.setLongitude(request.longitude());
		return endereco;
	}

}
