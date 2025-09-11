package com.nikoladesnica.mastermind.api.dto;

import java.util.UUID;

public record LeaderboardEntry(UUID accountId, String username, int score) {}
