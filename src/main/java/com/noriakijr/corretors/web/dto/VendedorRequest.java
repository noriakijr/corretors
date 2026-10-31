package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VendedorRequest(@NotBlank String nome) {
}
