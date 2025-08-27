package com.nikoladesnica.mastermind.domain;

import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.model.Game;
import com.nikoladesnica.mastermind.domain.model.GameStatus;
import com.nikoladesnica.mastermind.domain.ports.GameRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.domain.service.GameService;
import com.nikoladesnica.mastermind.domain.service.GuessEvaluator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;
import com.nikoladesnica.mastermind.infra.repo.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    private GameRepository repo;
    private SecretCodeGenerator generator;
    private GuessEvaluator evaluator;
    private GameProperties props;
    private GameService service;

    @BeforeEach
    void setup() {
        repo = new InMemoryGameRepository();
        // deterministic secret
        props = new GameProperties(
                4, 0, 7, 10, true, false,
                new GameProperties.RandomOrg("https://www.random.org/integers/", 1000)
        );
        generator = () -> new Code(List.of(0, 1, 3, 2), props.codeLength(), props.minDigit(), props.maxDigit(), props.allowDuplicates());
        evaluator = new GuessEvaluator();
        service = new GameService(repo, generator, evaluator, props);
    }

    @Test
    void startGame_createsAndPersists() {
        Game g = service.startGame();
        assertNotNull(g.id());
        assertEquals(GameStatus.IN_PROGRESS, g.status());
        assertEquals(props.attempts(), g.attemptsLeft());
        assertTrue(repo.findById(g.id()).isPresent());
    }

    @Test
    void submitGuess_decrementsAttempts_andCanWin() {
        Game g = service.startGame();
        int before = g.attemptsLeft();

        // wrong guess
        g = service.submitGuess(g.id(), List.of(0, 1, 2, 3));
        assertEquals(before - 1, g.attemptsLeft());
        assertEquals(GameStatus.IN_PROGRESS, g.status());
        assertEquals(1, g.history().size());

        // winning guess (matches the deterministic secret)
        g = service.submitGuess(g.id(), List.of(0, 1, 3, 2));
        assertEquals(GameStatus.WON, g.status());
        assertEquals(before - 2, g.attemptsLeft()); // winning guess still consumes an attempt
        assertEquals(2, g.history().size());
    }

    @Test
    void submitGuess_afterWin_doesNotChangeState() {
        Game g = service.startGame();
        g = service.submitGuess(g.id(), List.of(0, 1, 3, 2));
        assertEquals(GameStatus.WON, g.status());
        int attemptsAfterWin = g.attemptsLeft();
        int historyAfterWin = g.history().size();

        // further guesses are ignored
        g = service.submitGuess(g.id(), List.of(0, 1, 3, 2));
        assertEquals(GameStatus.WON, g.status());
        assertEquals(attemptsAfterWin, g.attemptsLeft());
        assertEquals(historyAfterWin, g.history().size());
    }

    @Test
    void submitGuess_outOfRange_throwsBadRequest() {
        Game g = service.startGame();
        var ex = assertThrows(
                com.nikoladesnica.mastermind.domain.errors.BadRequestException.class,
                () -> service.submitGuess(g.id(), List.of(0, 1, 2, 99))
        );
        assertTrue(ex.getMessage().contains("between 0 and 7"));
    }
}
