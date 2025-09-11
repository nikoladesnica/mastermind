package com.nikoladesnica.mastermind.domain.model;

import java.util.UUID;

public record Score(UUID accountId, int score) {}

// We leave this as the