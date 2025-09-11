package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Session;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    void put(Session session);
    Optional<Session> get(UUID token);
    void remove(UUID token);
}
