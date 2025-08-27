package com.nikoladesnica.mastermind.infra.repo;

import com.nikoladesnica.mastermind.domain.model.Room;
import com.nikoladesnica.mastermind.domain.ports.RoomRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoomRepository implements RoomRepository {
    private final Map<UUID, Room> store = new ConcurrentHashMap<>();

    @Override
    public void save(Room room) {
        store.put(room.roomId(), room);
    }

    @Override
    public Optional<Room> findById(UUID roomId) {
        return Optional.ofNullable(store.get(roomId));
    }
}
