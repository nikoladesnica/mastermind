package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Score;

import java.util.List;
import java.util.UUID;

public interface LeaderboardRepository {
    void increment(UUID accountId);
    int getScore(UUID accountId);
    List<Score> topK(int k);
}
