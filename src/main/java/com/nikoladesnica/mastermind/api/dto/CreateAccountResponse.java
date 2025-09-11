package com.nikoladesnica.mastermind.api.dto;

import java.util.UUID;

public record CreateAccountResponse(UUID accountId, String username) {}
