package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SeguradoraRequest(@NotBlank String nome) {
}
