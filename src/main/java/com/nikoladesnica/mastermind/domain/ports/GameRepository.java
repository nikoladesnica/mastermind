package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Game;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository {
    void save(Game game);
    Optional<Game> findById(UUID id);
}
