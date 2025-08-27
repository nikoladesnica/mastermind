package com.nikoladesnica.mastermind.api.dto;

import java.util.UUID;

public record JoinRoomResponse(UUID roomId, UUID playerId, String playerToken) {}
