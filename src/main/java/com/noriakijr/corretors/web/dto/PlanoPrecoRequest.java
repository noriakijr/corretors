package com.noriakijr.corretors.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PlanoPrecoRequest(@NotNull @PositiveOrZero Float valor) {
}
