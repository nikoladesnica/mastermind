package com.nikoladesnica.mastermind.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    public record Entry(Guess guess, Feedback feedback, Instant at) {}

    private final UUID id = UUID.randomUUID();
    private final Code secret;
    private final int maxAttempts;
    private int attemptsLeft;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private final List<Entry> history = new ArrayList<>();
    private final Instant startedAt = Instant.now();

    public Game(Code secret, int attempts) {
        this.secret = secret;
        this.maxAttempts = attempts;
        this.attemptsLeft = attempts;
    }

    public UUID id() { return id; }
    public Code secret() { return secret; }
    public int attemptsLeft() { return attemptsLeft; }
    public GameStatus status() { return status; }
    public List<Entry> history() { return List.copyOf(history); }
    public Instant startedAt() { return startedAt; }

    public void addEntry(Guess guess, Feedback feedback, boolean isWin) {
        if (status != GameStatus.IN_PROGRESS) return;
        history.add(new Entry(guess, feedback, Instant.now()));
        attemptsLeft--;
        if (isWin) status = GameStatus.WON;
        else if (attemptsLeft <= 0) status = GameStatus.LOST;
    };
}
