package com.nikoladesnica.mastermind.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Account {
    private final UUID id = UUID.randomUUID();
    private final String username; // unique
    private final byte[] passwordHash;
    private final byte[] salt;
    private final int iterations;
    private int wins;
    private int losses;
    private final Instant createdAt = Instant.now();
    private Instant lastLoginAt;

    public Account(String username, byte[] passwordHash, byte[] salt, int iterations) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.iterations = iterations;
    }

    public UUID id() { return id; }
    public String username() { return username; }
    public byte[] passwordHash() { return passwordHash; }
    public byte[] salt() { return salt; }
    public int iterations() { return iterations; }
    public int wins() { return wins; }
    public int losses() { return losses; }
    public Instant createdAt() { return createdAt; }
    public Instant lastLoginAt() { return lastLoginAt; }

    public void incrementWins() { wins++; }
    public void incrementLosses() { losses++; }
    public void setLastLoginAt(Instant t) { lastLoginAt = t; }
}
