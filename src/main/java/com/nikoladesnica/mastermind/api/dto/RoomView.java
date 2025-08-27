package com.nikoladesnica.mastermind.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoomView(
        UUID roomId,
        String state,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        List<PlayerSnapshot> players,
        List<LeaderboardEntry> leaderboard
) {
    public record PlayerSnapshot(
            UUID playerId,
            String name,
            String status,
            int attemptsLeft,
            List<HistoryEntry> history
    ) {}

    public record HistoryEntry(
            List<Integer> guess,
            int correctPositions,
            int correctNumbers,
            Instant at
    ) {}

    public record LeaderboardEntry(
            UUID playerId,
            String name,
            String status,
            int attemptsUsed,
            Long elapsedSeconds // null if not finished or not applicable
    ) {}
}
