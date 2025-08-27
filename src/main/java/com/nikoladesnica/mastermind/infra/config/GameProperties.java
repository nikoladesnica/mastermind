package com.nikoladesnica.mastermind.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mastermind")
public record GameProperties(
        int codeLength,
        int minDigit,
        int maxDigit,
        int attempts,
        boolean allowDuplicates,
        boolean useRandomOrg,
        RandomOrg randomOrg
) {
    public record RandomOrg(String baseUrl, int timeoutMs) {}
}