package com.nikoladesnica.mastermind.infra.repo;

import com.nikoladesnica.mastermind.domain.model.Session;
import com.nikoladesnica.mastermind.domain.ports.SessionRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionRepository implements SessionRepository {

    private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void put(Session session) {
        sessions.put(session.token(), session);
    }

    @Override
    public Optional<Session> get(UUID token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public void remove(UUID token) {
        sessions.remove(token);
    }
}
