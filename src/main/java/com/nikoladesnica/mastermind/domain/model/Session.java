package com.nikoladesnica.mastermind.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Session {
    private final UUID token = UUID.randomUUID();
    private final UUID accountId;
    private final Instant issuedAt = Instant.now();

    public Session(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID token() { return token; }
    public UUID accountId() { return accountId; }
    public Instant issuedAt() { return issuedAt; }
}
