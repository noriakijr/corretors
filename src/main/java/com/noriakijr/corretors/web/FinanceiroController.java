package com.noriakijr.corretors.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noriakijr.corretors.service.FinanceiroService;
import com.noriakijr.corretors.web.dto.DashboardResponse;
import com.noriakijr.corretors.web.dto.FinanceiroParcelaResponse;
import com.noriakijr.corretors.web.dto.FinanceiroResponse;

@RestController
@RequestMapping("/api")
public class FinanceiroController {

	private final FinanceiroService financeiro;

	public FinanceiroController(FinanceiroService financeiro) {
		this.financeiro = financeiro;
	}

	@GetMapping("/dashboard")
	public DashboardResponse dashboard() {
		return financeiro.dashboard();
	}

	@GetMapping("/financeiro")
	public FinanceiroResponse consultar(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate ate,
			@RequestParam(required = false) Long seguradoraId,
			@RequestParam(required = false) Long vendedorId) {
		return financeiro.consultar(de, ate, seguradoraId, vendedorId);
	}

	@PostMapping("/parcelas/{id}/pagar")
	public FinanceiroParcelaResponse pagar(@PathVariable Long id) {
		return financeiro.pagar(id);
	}

}
