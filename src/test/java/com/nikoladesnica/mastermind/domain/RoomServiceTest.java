package com.nikoladesnica.mastermind.domain;

import com.nikoladesnica.mastermind.domain.errors.BadRequestException;
import com.nikoladesnica.mastermind.domain.errors.ForbiddenException;
import com.nikoladesnica.mastermind.domain.errors.NotFoundException;
import com.nikoladesnica.mastermind.domain.model.*;
import com.nikoladesnica.mastermind.domain.ports.RoomRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.domain.service.GuessEvaluator;
import com.nikoladesnica.mastermind.domain.service.RoomService;
import com.nikoladesnica.mastermind.infra.config.GameProperties;
import com.nikoladesnica.mastermind.infra.repo.InMemoryRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {

    private RoomService service;
    private RoomRepository rooms;
    private SecretCodeGenerator generator;
    private GuessEvaluator evaluator;
    private GameProperties props;

    @BeforeEach
    void setUp() {
        rooms = new InMemoryRoomRepository();
        props = new GameProperties(
                4,   // codeLength
                0,   // minDigit
                7,   // maxDigit
                10,  // attempts
                true,// allowDuplicates
                false,// useRandomOrg
                null  // randomOrg
        );
        generator = () -> new Code(List.of(0, 1, 3, 2), 4, 0, 7, true);
        evaluator = new GuessEvaluator();
        service = new RoomService(rooms, generator, evaluator, props);
    }

    @Test
    void create_join_start_guess_winnerFinishesRoom() {
        Room room = service.createRoom();
        String hostToken = room.hostToken();
        UUID roomId = room.roomId();

        Player p1 = service.join(roomId, "Alice");
        Player p2 = service.join(roomId, "Bob");

        room = service.start(roomId, hostToken);
        assertEquals(RoomState.RUNNING, room.state());
        assertNotNull(room.startedAt());
        assertNotNull(room.secret());

        // p1 wrong guess
        room = service.guess(roomId, p1.id(), p1.token(), List.of(0, 0, 0, 0));
        assertEquals(RoomState.RUNNING, room.state());
        assertEquals(1, room.players().get(p1.id()).history().size());
        assertEquals(GameStatus.IN_PROGRESS, room.players().get(p1.id()).status());
        assertEquals(9, room.players().get(p1.id()).attemptsLeft());

        // p2 winning guess
        room = service.guess(roomId, p2.id(), p2.token(), List.of(0, 1, 3, 2));
        assertEquals(RoomState.FINISHED, room.state());
        assertEquals(GameStatus.WON, room.players().get(p2.id()).status());
        assertNotNull(room.players().get(p2.id()).finishedAt());
        assertNotNull(room.finishedAt());

        // After finish, p1 cannot guess anymore; state is frozen
        Room frozen = service.guess(roomId, p1.id(), p1.token(), List.of(0, 1, 3, 2));
        assertEquals(RoomState.FINISHED, frozen.state());
        assertEquals(1, frozen.players().get(p1.id()).history().size());
        assertEquals(GameStatus.IN_PROGRESS, frozen.players().get(p1.id()).status());
    }

    @Test
    void join_after_start_fails() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        String host = room.hostToken();

        service.join(roomId, "P1");
        service.start(roomId, host);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.join(roomId, "LateComer"));
        assertTrue(ex.getMessage().toLowerCase().contains("not joinable"));
    }

    @Test
    void start_requires_valid_hostToken_and_atLeastOnePlayer() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();

        // No players -> BadRequest
        assertThrows(BadRequestException.class, () -> service.start(roomId, room.hostToken()));

        // Add a player, wrong host token -> Forbidden
        service.join(roomId, "P1");
        assertThrows(ForbiddenException.class, () -> service.start(roomId, "bad-token"));

        // Correct host -> starts
        Room started = service.start(roomId, room.hostToken());
        assertEquals(RoomState.RUNNING, started.state());
    }

    @Test
    void guess_validates_tokens_and_digits() {
        Room created = service.createRoom();
        UUID roomId = created.roomId();
        Player p = service.join(roomId, "P1");
        service.start(roomId, created.hostToken());

        // Bad token -> Forbidden
        assertThrows(ForbiddenException.class,
                () -> service.guess(roomId, p.id(), "WRONG", List.of(0, 1, 2, 3)));

        // Bad length -> BadRequest (RoomService.validateDigits)
        assertThrows(BadRequestException.class,
                () -> service.guess(roomId, p.id(), p.token(), List.of(0, 1, 2)));

        // Out of range -> BadRequest (RoomService.validateDigits)
        assertThrows(BadRequestException.class,
                () -> service.guess(roomId, p.id(), p.token(), List.of(0, 1, 2, 9)));
    }

    @Test
    void leave_in_waiting_removes_player() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();

        Player p1 = service.join(roomId, "Alice");
        Player p2 = service.join(roomId, "Bob");
        assertEquals(2, service.get(roomId).players().size());

        Room afterLeave = service.leave(roomId, p1.id(), p1.token());
        assertEquals(RoomState.WAITING, afterLeave.state());
        assertNull(afterLeave.players().get(p1.id())); // removed from lobby
        assertEquals(1, afterLeave.players().size());
        assertNotNull(afterLeave.players().get(p2.id())); // Bob remains
    }

    @Test
    void leave_in_running_single_player_marks_lost_and_finishes() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        String hostToken = room.hostToken();

        Player p1 = service.join(roomId, "Solo");
        service.start(roomId, hostToken);

        Room afterLeave = service.leave(roomId, p1.id(), p1.token());
        assertEquals(RoomState.FINISHED, afterLeave.state());
        Player pSnapshot = afterLeave.players().get(p1.id());
        assertEquals(GameStatus.LOST, pSnapshot.status());
        assertNotNull(pSnapshot.finishedAt());
        assertNotNull(afterLeave.finishedAt());
    }

    @Test
    void leave_requires_valid_token_and_existing_player() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        Player p = service.join(roomId, "P");

        // wrong token -> Forbidden
        assertThrows(ForbiddenException.class,
                () -> service.leave(roomId, p.id(), "WRONG"));

        // unknown player -> NotFound
        assertThrows(NotFoundException.class,
                () -> service.leave(roomId, UUID.randomUUID(), "anything"));
    }

    @Test
    void leave_after_finished_is_noop() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        String hostToken = room.hostToken();
        Player p = service.join(roomId, "Winner");

        // Start and win immediately
        service.start(roomId, hostToken);
        service.guess(roomId, p.id(), p.token(), List.of(0, 1, 3, 2)); // winning digits from fixed generator
        Room finished = service.get(roomId);
        assertEquals(RoomState.FINISHED, finished.state());
        Player winner = finished.players().get(p.id());
        assertEquals(GameStatus.WON, winner.status());

        // Leave after finished -> no-op
        Room afterLeave = service.leave(roomId, p.id(), p.token());
        assertEquals(RoomState.FINISHED, afterLeave.state());
        assertEquals(GameStatus.WON, afterLeave.players().get(p.id()).status());
    }

    @Test
    void promoteHost_allows_waiting_player_to_rotate_host_token() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();

        Player p1 = service.join(roomId, "A");
        Player p2 = service.join(roomId, "B");

        String oldToken = service.get(roomId).hostToken();
        String newToken = service.promoteHost(roomId, p1.id(), p1.token());
        assertNotEquals(oldToken, newToken);

        // old token cannot start
        assertThrows(com.nikoladesnica.mastermind.domain.errors.ForbiddenException.class,
                () -> service.start(roomId, oldToken));

        // new token can start
        Room started = service.start(roomId, newToken);
        assertEquals(RoomState.RUNNING, started.state());
    }

    @Test
    void assignHost_allows_current_host_to_pick_specific_player() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        String hostToken = room.hostToken();

        Player p1 = service.join(roomId, "A");
        Player p2 = service.join(roomId, "B");

        String newToken = service.assignHost(roomId, hostToken, p2.id());
        assertNotEquals(hostToken, newToken);

        // old token invalid
        assertThrows(com.nikoladesnica.mastermind.domain.errors.ForbiddenException.class,
                () -> service.start(roomId, hostToken));

        // new token valid
        Room started = service.start(roomId, newToken);
        assertEquals(RoomState.RUNNING, started.state());
    }

    @Test
    void kick_in_lobby_removes_player() {
        Room room = service.createRoom();
        UUID roomId = room.roomId();
        String hostToken = room.hostToken();

        Player a = service.join(roomId, "A");
        Player b = service.join(roomId, "B");

        Room after = service.kick(roomId, hostToken, b.id());

        assertEquals(RoomState.WAITING, after.state());
        assertTrue(after.players().containsKey(a.id()));
        assertFalse(after.players().containsKey(b.id()));
    }
}
