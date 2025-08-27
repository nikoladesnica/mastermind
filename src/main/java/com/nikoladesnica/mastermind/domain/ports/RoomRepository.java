package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Room;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {
    void save(Room room);
    Optional<Room> findById(UUID roomId);
}
