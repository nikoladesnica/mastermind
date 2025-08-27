package com.nikoladesnica.mastermind.infra.repo;

import com.nikoladesnica.mastermind.domain.model.Game;
import com.nikoladesnica.mastermind.domain.ports.GameRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryGameRepository implements GameRepository {
    private final Map<UUID, Game> store = new ConcurrentHashMap<>();

    @Override public void save(Game game) { store.put(game.id(), game); }
    @Override public Optional<Game> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
}
