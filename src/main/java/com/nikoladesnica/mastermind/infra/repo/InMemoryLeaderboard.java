package com.nikoladesnica.mastermind.infra.repo;

import com.nikoladesnica.mastermind.domain.model.Score;
import com.nikoladesnica.mastermind.domain.ports.LeaderboardRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class InMemoryLeaderboard implements LeaderboardRepository {

    private static final class Node {
        final UUID accountId;
        final int score;
        Node(UUID accountId, int score) { this.accountId = accountId; this.score = score; }
    }

    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();
    private final PriorityQueue<Node> heap = new PriorityQueue<>(
            Comparator.<Node>comparingInt(n -> n.score)
                    .thenComparing(n -> n.accountId)
    );
    private final int k;

    public InMemoryLeaderboard(int k) {
        if (k <= 0) throw new IllegalArgumentException("k must be > 0");
        this.k = k;
    }

    @Override
    public synchronized void increment(UUID accountId) {
        int newScore = scores.merge(accountId, 1, Integer::sum);
        if (heap.size() < k) {
            heap.offer(new Node(accountId, newScore));
            return;
        }
        Node smallest = heap.peek();
        if (smallest != null && newScore > smallest.score) {
            heap.offer(new Node(accountId, newScore));
            while (heap.size() > k) heap.poll();
        }
    }

    @Override
    public synchronized int getScore(UUID accountId) {
        return scores.getOrDefault(accountId, 0);
    }

    @Override
    public synchronized List<Score> topK(int requested) {
        PriorityQueue<Node> max = new PriorityQueue<>(
                Comparator.<Node>comparingInt(n -> n.score).reversed()
                        .thenComparing(n -> n.accountId)
        );
        max.addAll(heap);

        List<Score> out = new ArrayList<>(Math.min(k, requested));
        Set<UUID> seen = new HashSet<>();

        while (!max.isEmpty() && out.size() < requested) {
            Node n = max.poll();
            Integer cur = scores.get(n.accountId);
            if (cur == null) continue;
            if (seen.contains(n.accountId)) continue;
            if (cur != n.score) continue; // prune stale
            out.add(new Score(n.accountId, cur));
            seen.add(n.accountId);
        }
        return out;
    }
}
