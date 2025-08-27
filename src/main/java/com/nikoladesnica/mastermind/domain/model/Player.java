package com.nikoladesnica.mastermind.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {
    private final UUID id = UUID.randomUUID();
    private final String name;
    private final String token;

    private int attemptsLeft;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private Instant finishedAt; // when player WON or LOST
    private final List<Game.Entry> history = new ArrayList<>();

    public Player(String name, String token, int attempts) {
        this.name = (name == null || name.isBlank()) ? ("Player-" + id.toString().substring(0, 8)) : name.trim();
        this.token = token;
        this.attemptsLeft = attempts;
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public String token() { return token; }

    public int attemptsLeft() { return attemptsLeft; }
    public void decrementAttempt() { this.attemptsLeft--; }

    public GameStatus status() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public Instant finishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public List<Game.Entry> history() { return history; }
}
