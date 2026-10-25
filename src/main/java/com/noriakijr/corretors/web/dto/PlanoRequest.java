package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanoRequest(@NotBlank String nome, String descricao) {
}
