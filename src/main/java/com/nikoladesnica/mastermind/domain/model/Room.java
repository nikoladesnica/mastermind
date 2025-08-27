package com.nikoladesnica.mastermind.domain.model;

import java.time.Instant;
import java.util.*;

public class Room {
    private final UUID roomId = UUID.randomUUID();
    private String hostToken; // ← was final; now mutable
    private final Instant createdAt = Instant.now();

    private RoomState state = RoomState.WAITING;
    private Instant startedAt;
    private Instant finishedAt;

    // Secret code for the race (set on start)
    private Code secret;

    // Keep insertion order for a stable leaderboard display
    private final Map<UUID, Player> players = new LinkedHashMap<>();

    public Room(String hostToken) {
        this.hostToken = hostToken;
    }

    public UUID roomId() { return roomId; }
    public String hostToken() { return hostToken; }
    public void setHostToken(String hostToken) { this.hostToken = hostToken; } // ← added

    public RoomState state() { return state; }
    public void setState(RoomState state) { this.state = state; }

    public Instant createdAt() { return createdAt; }
    public Instant startedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant finishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Code secret() { return secret; }
    public void setSecret(Code secret) { this.secret = secret; }

    public Map<UUID, Player> players() { return players; }

    public boolean allFinished() {
        return players.values().stream().allMatch(p -> p.status() != GameStatus.IN_PROGRESS);
    }
}
