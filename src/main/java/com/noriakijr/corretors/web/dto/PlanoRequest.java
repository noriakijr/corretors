package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record PlanoRequest(@NotBlank String nome, String descricao, @PositiveOrZero Float valor) {
}
