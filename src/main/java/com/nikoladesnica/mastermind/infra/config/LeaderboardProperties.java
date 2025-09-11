package com.nikoladesnica.mastermind.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "leaderboard")
public record LeaderboardProperties(int topK) {}
