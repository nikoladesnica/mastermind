package com.nikoladesnica.mastermind.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GuessRequest(
        @NotNull @Size(min = 4, max = 4) List<Integer> digits
) {}
