package com.nikoladesnica.mastermind.api;

import com.nikoladesnica.mastermind.api.dto.LeaderboardEntry;
import com.nikoladesnica.mastermind.domain.ports.AccountRepository;
import com.nikoladesnica.mastermind.domain.ports.LeaderboardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LeaderboardController {

    private final LeaderboardRepository leaderboard;
    private final AccountRepository accounts;

    public LeaderboardController(LeaderboardRepository leaderboard, AccountRepository accounts) {
        this.leaderboard = leaderboard;
        this.accounts = accounts;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> get(@RequestParam(name = "top", required = false) Integer top) {
        int k = (top == null || top <= 0) ? 10 : top;
        var scores = leaderboard.topK(k);
        var out = scores.stream().map(s -> {
            var acc = accounts.findById(s.accountId()).orElse(null);
            var username = acc != null ? acc.username() : "unknown";
            return new LeaderboardEntry(s.accountId(), username, s.score());
        }).toList();
        return ResponseEntity.ok(out);
    }
}
