package com.nikoladesnica.mastermind.api.dto;

import java.util.UUID;

public record CreateRoomResponse(UUID roomId, String hostToken) {}
