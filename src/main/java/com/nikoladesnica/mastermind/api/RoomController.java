package com.nikoladesnica.mastermind.api;

import com.nikoladesnica.mastermind.api.dto.*;
import com.nikoladesnica.mastermind.domain.model.Game;
import com.nikoladesnica.mastermind.domain.model.GameStatus;
import com.nikoladesnica.mastermind.domain.model.Player;
import com.nikoladesnica.mastermind.domain.model.Room;
import com.nikoladesnica.mastermind.domain.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService service;

    public RoomController(RoomService service) {
        this.service = service;
    }

    @PostMapping("/rooms")
    public ResponseEntity<CreateRoomResponse> createRoom() {
        Room room = service.createRoom();
        return ResponseEntity.ok(new CreateRoomResponse(room.roomId(), room.hostToken()));
    }

    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(@PathVariable UUID roomId,
                                                     @Valid @RequestBody JoinRoomRequest req) {
        Player p = service.join(roomId, req == null ? null : req.name());
        return ResponseEntity.ok(new JoinRoomResponse(roomId, p.id(), p.token()));
    }

    @PostMapping("/rooms/{roomId}/start")
    public ResponseEntity<RoomView> start(@PathVariable UUID roomId,
                                          @RequestHeader("X-Host-Token") String hostToken) {
        Room room = service.start(roomId, hostToken);
        return ResponseEntity.ok(Mappers.view(room));
    }

    @PostMapping("/rooms/{roomId}/guesses")
    public ResponseEntity<RoomView> guess(@PathVariable UUID roomId,
                                          @RequestHeader("X-Player-Id") UUID playerId,
                                          @RequestHeader("X-Player-Token") String playerToken,
                                          @Valid @RequestBody GuessRequest req) {
        Room room = service.guess(roomId, playerId, playerToken, req.digits());
        return ResponseEntity.ok(Mappers.view(room));
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<RoomView> leave(@PathVariable UUID roomId,
                                          @RequestHeader("X-Player-Id") UUID playerId,
                                          @RequestHeader("X-Player-Token") String playerToken) {
        Room room = service.leave(roomId, playerId, playerToken);
        return ResponseEntity.ok(Mappers.view(room));
    }

    @PostMapping("/rooms/{roomId}/kick/{playerId}")
    public ResponseEntity<RoomView> kick(@PathVariable UUID roomId,
                                         @PathVariable UUID playerId,
                                         @RequestHeader("X-Host-Token") String hostToken) {
        Room room = service.kick(roomId, hostToken, playerId);
        return ResponseEntity.ok(Mappers.view(room));
    }

    /** Any waiting player can claim the host role (when original host disappears). */
    @PostMapping("/rooms/{roomId}/promote-host")
    public ResponseEntity<CreateRoomResponse> promoteHost(@PathVariable UUID roomId,
                                                          @RequestHeader("X-Player-Id") UUID playerId,
                                                          @RequestHeader("X-Player-Token") String playerToken) {
        String newHostToken = service.promoteHost(roomId, playerId, playerToken);
        return ResponseEntity.ok(new CreateRoomResponse(roomId, newHostToken));
    }

    /** Current host assigns host role to a specific player (still waiting). */
    @PostMapping("/rooms/{roomId}/assign-host/{targetPlayerId}")
    public ResponseEntity<CreateRoomResponse> assignHost(@PathVariable UUID roomId,
                                                         @PathVariable UUID targetPlayerId,
                                                         @RequestHeader("X-Host-Token") String hostToken) {
        String newHostToken = service.assignHost(roomId, hostToken, targetPlayerId);
        return ResponseEntity.ok(new CreateRoomResponse(roomId, newHostToken));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomView> get(@PathVariable UUID roomId) {
        return ResponseEntity.ok(Mappers.view(service.get(roomId)));
    }

    // --- Mapper ---
    static class Mappers {
        static RoomView view(Room r) {
            var players = r.players().values().stream()
                    .map(Mappers::player)
                    .toList();

            var leaderboard = r.players().values().stream()
                    .map(p -> {
                        int attemptsUsed = p.history().size();
                        Long elapsed = null;
                        if (r.startedAt() != null && p.finishedAt() != null) {
                            elapsed = Duration.between(r.startedAt(), p.finishedAt()).getSeconds();
                        } else if (r.startedAt() != null && r.finishedAt() != null && p.finishedAt() == null) {
                            // room finished but player didn't (e.g., lost due to other winner)
                            elapsed = Duration.between(r.startedAt(), r.finishedAt()).getSeconds();
                        }
                        return new RoomView.LeaderboardEntry(
                                p.id(), p.name(), p.status().name(), attemptsUsed, elapsed
                        );
                    })
                    .sorted(leaderboardComparator())
                    .toList();

            return new RoomView(
                    r.roomId(),
                    r.state().name(),
                    r.createdAt(),
                    r.startedAt(),
                    r.finishedAt(),
                    players,
                    leaderboard
            );
        }

        private static RoomView.PlayerSnapshot player(Player p) {
            List<RoomView.HistoryEntry> history = p.history().stream()
                    .map(Mappers::history)
                    .toList();
            return new RoomView.PlayerSnapshot(
                    p.id(), p.name(), p.status().name(), p.attemptsLeft(), history
            );
        }

        private static RoomView.HistoryEntry history(Game.Entry e) {
            return new RoomView.HistoryEntry(
                    e.guess().digits(), e.feedback().correctPositions(), e.feedback().correctNumbers(), e.at()
            );
        }

        // Sort by: WON first, then IN_PROGRESS, then LOST.
        // Among WON: fewer attemptsUsed wins, then shorter elapsedSeconds.
        private static Comparator<RoomView.LeaderboardEntry> leaderboardComparator() {
            return Comparator
                    .comparing((RoomView.LeaderboardEntry le) -> rank(le.status()))
                    .thenComparingInt(RoomView.LeaderboardEntry::attemptsUsed)
                    .thenComparing((RoomView.LeaderboardEntry le) -> le.elapsedSeconds() == null ? Long.MAX_VALUE : le.elapsedSeconds());
        }

        private static int rank(String status) {
            return switch (status) {
                case "WON" -> 0;
                case "IN_PROGRESS" -> 1;
                case "LOST" -> 2;
                default -> 3;
            };
        }
    }
}
