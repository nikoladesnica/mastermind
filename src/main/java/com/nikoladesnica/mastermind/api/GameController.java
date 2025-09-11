package com.nikoladesnica.mastermind.api;

import com.nikoladesnica.mastermind.api.dto.GameView;
import com.nikoladesnica.mastermind.api.dto.GuessRequest;
import com.nikoladesnica.mastermind.api.dto.NewGameRequest;
import com.nikoladesnica.mastermind.api.dto.NewGameResponse;
import com.nikoladesnica.mastermind.domain.model.Game;
import com.nikoladesnica.mastermind.domain.model.GameStatus;
import com.nikoladesnica.mastermind.domain.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Accounts & Leaderboard Extension
import com.nikoladesnica.mastermind.domain.service.AccountService;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService service;
    private final AccountService accountService;

    public GameController(GameService service, AccountService accountService) {
        this.service = service;
        this.accountService = accountService;
    }

    @PostMapping("/games")
    public ResponseEntity<NewGameResponse> start(@RequestBody(required = false) NewGameRequest req) {
        // For v1, we ignore overrides; could add dynamic config later.
        Game game = service.startGame();
        return ResponseEntity.ok(new NewGameResponse(game.id(), game.attemptsLeft(), game.status().name()));
    }

    @PostMapping("/games/{id}/guesses")
    public ResponseEntity<GameView> guess(@PathVariable UUID id,
                                          @RequestHeader(value = "X-Session-Token", required = false) UUID sessionToken,
                                          @Valid @RequestBody GuessRequest req) {
        Game game = service.submitGuess(id, req.digits());

        if (sessionToken != null && game.status() == GameStatus.WON) {
            UUID accountId = accountService.accountIdFromSession(sessionToken);
            accountService.recordWin(accountId);
        }
        
        return ResponseEntity.ok(Mappers.view(game));
    }

    @GetMapping("/games/{id}")
    public ResponseEntity<GameView> get(@PathVariable UUID id) {
        return ResponseEntity.ok(Mappers.view(service.getGame(id)));
    }

    // Tiny mapper as a nested helper (keeps API separate from domain)
    static class Mappers {
        static GameView view(Game g) {
            boolean canGuess = g.status() == GameStatus.IN_PROGRESS;
            String message = canGuess ? null : "Game finished (" + g.status().name() + "). Start a new game.";

            var history = g.history().stream()
                    .map(e -> new GameView.HistoryEntry(
                            e.guess().digits(),
                            e.feedback().correctPositions(),
                            e.feedback().correctNumbers(),
                            e.at()))
                    .toList();

            return new GameView(
                    g.id(),
                    g.status().name(),
                    g.attemptsLeft(),
                    canGuess,
                    message,
                    history.size(),
                    history
            );
        }
    }
}
