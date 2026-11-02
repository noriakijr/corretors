package com.noriakijr.corretors.web.dto;

import com.noriakijr.corretors.model.TipoComissao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

public record ComissaoRequest(
		TipoComissao tipoComissao,
		@DecimalMin(value = "0", message = "percentual de comissao invalido")
		@DecimalMax(value = "100", message = "percentual de comissao nao pode passar de 100") Double percentual,
		@PositiveOrZero(message = "valor fixo da comissao invalido") Double valorFixo) {
}
