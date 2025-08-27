package com.nikoladesnica.mastermind.api.dto;

// Optional: allow overrides like allowDuplicates; keep empty for defaults
public record NewGameRequest(Boolean allowDuplicates, Integer attempts) {}
