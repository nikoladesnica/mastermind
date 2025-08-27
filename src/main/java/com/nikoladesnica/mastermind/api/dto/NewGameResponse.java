package com.nikoladesnica.mastermind.api.dto;

import java.util.UUID;

public record NewGameResponse(UUID gameId, int attemptsLeft, String status) {}
