package com.nikoladesnica.mastermind.domain.service;

import com.nikoladesnica.mastermind.domain.errors.BadRequestException;
import com.nikoladesnica.mastermind.domain.errors.NotFoundException;
import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.model.Feedback;
import com.nikoladesnica.mastermind.domain.model.Game;
import com.nikoladesnica.mastermind.domain.model.GameStatus;
import com.nikoladesnica.mastermind.domain.model.Guess;
import com.nikoladesnica.mastermind.domain.ports.GameRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;

import java.util.List;
import java.util.UUID;

public class GameService {
    private final GameRepository repo;
    private final SecretCodeGenerator generator;
    private final GuessEvaluator evaluator;
    private final GameProperties props;

    public GameService(GameRepository repo, SecretCodeGenerator generator, GuessEvaluator evaluator, GameProperties props) {
        this.repo = repo;
        this.generator = generator;
        this.evaluator = evaluator;
        this.props = props;
    }

    public Game startGame() {
        Code code = generator.generate();
        Game game = new Game(code, props.attempts());
        repo.save(game);
        return game;
    }

    public Game submitGuess(UUID id, List<Integer> digits) {
        Game game = repo.findById(id).orElseThrow(() -> new NotFoundException("Game not found"));
        if (game.status() != GameStatus.IN_PROGRESS) return game;

        validateDigits(digits);

        Code secret = game.secret();
        Guess guess = new Guess(digits);
        Feedback fb = evaluator.evaluate(secret, guess);
        boolean win = fb.correctPositions() == props.codeLength();
        game.addEntry(guess, fb, win);
        repo.save(game);
        return game;
    }

    public Game getGame(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Game not found"));
    }

    private void validateDigits(List<Integer> digits) {
        if (digits == null) {
            throw new BadRequestException("Digits must not be null");
        }
        if (digits.size() != props.codeLength()) {
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
