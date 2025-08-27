package com.nikoladesnica.mastermind.domain.service;

import com.nikoladesnica.mastermind.domain.errors.BadRequestException;
import com.nikoladesnica.mastermind.domain.errors.ForbiddenException;
import com.nikoladesnica.mastermind.domain.errors.NotFoundException;
import com.nikoladesnica.mastermind.domain.model.*;
import com.nikoladesnica.mastermind.domain.ports.RoomRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoomService {

    private final RoomRepository rooms;
    private final SecretCodeGenerator generator;
    private final GuessEvaluator evaluator;
    private final GameProperties props;

    public RoomService(RoomRepository rooms,
                       SecretCodeGenerator generator,
                       GuessEvaluator evaluator,
                       GameProperties props) {
        this.rooms = rooms;
        this.generator = generator;
        this.evaluator = evaluator;
        this.props = props;
    }

    public Room createRoom() {
        String hostToken = UUID.randomUUID().toString();
        Room room = new Room(hostToken);
        rooms.save(room);
        return room;
    }

    public Player join(UUID roomId, String name) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            if (room.state() != RoomState.WAITING) {
                throw new BadRequestException("Room is not joinable");
            }
            String playerToken = UUID.randomUUID().toString();
            Player p = new Player(name, playerToken, props.attempts());
            room.players().put(p.id(), p);
            rooms.save(room);
            return p;
        }
    }

    public Room start(UUID roomId, String hostToken) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            if (!room.hostToken().equals(hostToken)) {
                throw new ForbiddenException("Invalid host token");
            }
            if (room.state() != RoomState.WAITING) {
                return room; // idempotent
            }
            if (room.players().isEmpty()) {
                throw new BadRequestException("At least one player must join to start");
            }
            room.setSecret(generator.generate());
            room.setStartedAt(Instant.now());
            room.setState(RoomState.RUNNING);

            room.players().values().forEach(p -> {
                p.history().clear();
                p.setStatus(GameStatus.IN_PROGRESS);
                // attempts were set on construction from props
            });

            rooms.save(room);
            return room;
        }
    }

    public Room guess(UUID roomId, UUID playerId, String playerToken, List<Integer> digits) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));

        synchronized (room) {
            if (room.state() != RoomState.RUNNING) {
                return room; // frozen until start or already finished
            }
            Player p = room.players().get(playerId);
            if (p == null) throw new NotFoundException("Player not found");
            if (!p.token().equals(playerToken)) throw new ForbiddenException("Invalid player token");
            if (p.status() != GameStatus.IN_PROGRESS) {
                return room; // player already finished; no-op
            }

            validateDigits(digits);

            Code secret = room.secret();
            Guess guess = new Guess(digits);
            Feedback fb = evaluator.evaluate(secret, guess);

            p.history().add(new Game.Entry(guess, fb, Instant.now()));

            p.decrementAttempt();
            boolean win = fb.correctPositions() == props.codeLength();
            if (win) {
                p.setStatus(GameStatus.WON);
                p.setFinishedAt(Instant.now());
                if (room.state() != RoomState.FINISHED) {
                    room.setFinishedAt(Instant.now());
                    room.setState(RoomState.FINISHED);
                }
            } else if (p.attemptsLeft() <= 0) {
                p.setStatus(GameStatus.LOST);
                p.setFinishedAt(Instant.now());
                if (room.allFinished()) {
                    room.setFinishedAt(Instant.now());
                    room.setState(RoomState.FINISHED);
                }
            }

            rooms.save(room);
            return room;
        }
    }

    /**
     * Player leaves a room.
     * - WAITING: remove player from the lobby.
     * - RUNNING: mark as LOST (keeps history/leaderboard) and finish room if everyone is done.
     * - FINISHED: no-op.
     */
    public Room leave(UUID roomId, UUID playerId, String playerToken) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            Player p = room.players().get(playerId);
            if (p == null) throw new NotFoundException("Player not found");
            if (!p.token().equals(playerToken)) throw new ForbiddenException("Invalid player token");

            switch (room.state()) {
                case WAITING -> {
                    // Remove from lobby entirely so they no longer appear
                    room.players().remove(playerId);
                }
                case RUNNING -> {
                    // Mark as LOST only if still playing; keep them visible with final state
                    if (p.status() == GameStatus.IN_PROGRESS) {
                        p.setStatus(GameStatus.LOST);
                        p.setFinishedAt(Instant.now());
                    }
                    if (room.allFinished()) {
                        room.setFinishedAt(Instant.now());
                        room.setState(RoomState.FINISHED);
                    }
                }
                case FINISHED -> {
                    // no-op, allow client to fetch final snapshot
                }
            }

            rooms.save(room);
            return room;
        }
    }

    public Room kick(UUID roomId, String hostToken, UUID targetPlayerId) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            if (room.state() != RoomState.WAITING) {
                throw new BadRequestException("Kick is allowed only in the lobby");
            }
            if (!room.hostToken().equals(hostToken)) {
                throw new ForbiddenException("Invalid host token");
            }
            Player removed = room.players().remove(targetPlayerId);
            if (removed == null) {
                throw new NotFoundException("Player not found");
            }
            rooms.save(room);
            return room;
        }
    }

    /**
     * Any waiting player can claim/rotate the host token to themselves.
     * Use when the original host disappears during WAITING.
     * Returns the NEW host token.
     */
    public String promoteHost(UUID roomId, UUID playerId, String playerToken) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            if (room.state() != RoomState.WAITING) {
                throw new BadRequestException("Can only change host while waiting");
            }
            Player p = room.players().get(playerId);
            if (p == null) throw new NotFoundException("Player not found");
            if (!p.token().equals(playerToken)) throw new ForbiddenException("Invalid player token");

            String newToken = UUID.randomUUID().toString();
            room.setHostToken(newToken);
            rooms.save(room);
            return newToken;
        }
    }

    /**
     * Current host assigns host role to a specific player (still in WAITING).
     * Returns the NEW host token (old token becomes invalid immediately).
     */
    public String assignHost(UUID roomId, String currentHostToken, UUID targetPlayerId) {
        Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
        synchronized (room) {
            if (room.state() != RoomState.WAITING) {
                throw new BadRequestException("Can only change host while waiting");
            }
            if (!room.hostToken().equals(currentHostToken)) {
                throw new ForbiddenException("Invalid host token");
            }
            Player target = room.players().get(targetPlayerId);
            if (target == null) throw new NotFoundException("Player not found");

            String newToken = UUID.randomUUID().toString();
            room.setHostToken(newToken);
            rooms.save(room);
            return newToken;
        }
    }

    public Room get(UUID roomId) {
        return rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room not found"));
    }

    private void validateDigits(List<Integer> digits) {
        if (digits == null || digits.size() != props.codeLength()) {
            throw new BadRequestException("Exactly " + props.codeLength() + " digits are required");
        }
        int min = props.minDigit();
        int max = props.maxDigit();
        for (Integer d : digits) {
            if (d == null || d < min || d > max) {
                throw new BadRequestException("Each digit must be between " + min + " and " + max);
            }
        }
    }
}
