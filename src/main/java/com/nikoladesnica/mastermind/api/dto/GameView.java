package com.nikoladesnica.mastermind.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameView(
        UUID gameId,
        String status,
        int attemptsLeft,
        boolean canGuess,
        String message,              // null while IN_PROGRESS; present after WIN/LOSS
        int historyCount,
        List<HistoryEntry> history
) {
    public record HistoryEntry(List<Integer> guess, int correctPositions, int correctNumbers, Instant at) {}
}
