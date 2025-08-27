package com.nikoladesnica.mastermind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "mastermind.code-length=4",
        "mastermind.min-digit=0",
        "mastermind.max-digit=7",
        "mastermind.attempts=10",
        "mastermind.allow-duplicates=true",
        "mastermind.use-random-org=false",
        "mastermind.random-org.timeout-ms=500",
        "mastermind.random-org.base-url=https://www.random.org/integers/"
})
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void create_join_start_guess_finish_leaderboard() throws Exception {
        // Create
        String createJson = mvc.perform(post("/api/rooms")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", not(emptyString())))
                .andExpect(jsonPath("$.hostToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String hostToken = create.get("hostToken").asText();

        // Join P1
        String join1Json = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId", not(emptyString())))
                .andExpect(jsonPath("$.playerToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        JsonNode j1 = mapper.readTree(join1Json);
        String p1Id = j1.get("playerId").asText();
        String p1Token = j1.get("playerToken").asText();

        // Join P2
        String join2Json = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId", not(emptyString())))
                .andExpect(jsonPath("$.playerToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        JsonNode j2 = mapper.readTree(join2Json);
        String p2Id = j2.get("playerId").asText();
        String p2Token = j2.get("playerToken").asText();

        // Start (host)
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Host-Token", hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")));

        // P1 wrong guess
        String afterP1Guess = mvc.perform(post("/api/rooms/{id}/guesses", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", p1Id)
                        .header("X-Player-Token", p1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digits\":[0,0,0,0]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")))
                .andReturn().getResponse().getContentAsString();
        JsonNode snap1 = mapper.readTree(afterP1Guess);
        int p1HistoryAfterFirstGuess = historyCountFor(snap1, p1Id);

        // P2 winning guess (secret is [0,1,3,2])
        mvc.perform(post("/api/rooms/{id}/guesses", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", p2Id)
                        .header("X-Player-Token", p2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digits\":[0,1,3,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("FINISHED")))
                .andExpect(jsonPath("$.leaderboard[0].name", is("Bob")))
                .andExpect(jsonPath("$.leaderboard[0].status", anyOf(is("WON"), is("WON_BY_TIME"))));

        // Further guess by P1 is ignored; state remains FINISHED and history length unchanged
        String afterFrozen = mvc.perform(post("/api/rooms/{id}/guesses", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", p1Id)
                        .header("X-Player-Token", p1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digits\":[0,1,3,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("FINISHED")))
                .andReturn().getResponse().getContentAsString();
        JsonNode frozen = mapper.readTree(afterFrozen);
        int p1HistoryFrozen = historyCountFor(frozen, p1Id);
        assertEquals(p1HistoryAfterFirstGuess, p1HistoryFrozen);
    }

    @Test
    void invalid_guess_range_returns400_apiError() throws Exception {
        String createJson = mvc.perform(post("/api/rooms")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String hostToken = create.get("hostToken").asText();

        String joinJson = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode j = mapper.readTree(joinJson);
        String pid = j.get("playerId").asText();
        String ptoken = j.get("playerToken").asText();

        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Host-Token", hostToken))
                .andExpect(status().isOk());

        mvc.perform(post("/api/rooms/{id}/guesses", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", pid)
                        .header("X-Player-Token", ptoken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"digits\":[0,1,2,9]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("between 0 and 7")))
                .andExpect(jsonPath("$.path", containsString("/api/rooms/" + roomId + "/guesses")));
    }

    @Test
    void room_not_found_returns404() throws Exception {
        String fakeRoom = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        mvc.perform(get("/api/rooms/{id}", fakeRoom)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Room not found")));
    }

    @Test
    void start_with_wrong_host_token_returns403() throws Exception {
        String createJson = mvc.perform(post("/api/rooms")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();

        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Host-Token", "WRONG"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("Invalid host token")));
    }

    @Test
    void leave_in_lobby_removes_player_from_list() throws Exception {
        // Create room
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();

        // Join P1 + P2
        String join1Json = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode j1 = mapper.readTree(join1Json);
        String p1Id = j1.get("playerId").asText();
        String p1Token = j1.get("playerToken").asText();

        mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob\"}"))
                .andExpect(status().isOk());

        // P1 leaves while WAITING -> removed from players
        mvc.perform(post("/api/rooms/{id}/leave", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", p1Id)
                        .header("X-Player-Token", p1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("WAITING")))
                .andExpect(jsonPath("$.players[*].playerId", not(hasItem(p1Id))));
    }

    @Test
    void leave_in_running_single_player_marks_lost_and_finishes() throws Exception {
        // Create + join single player
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String hostToken = create.get("hostToken").asText();

        String joinJson = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Solo\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode j = mapper.readTree(joinJson);
        String pId = j.get("playerId").asText();
        String pToken = j.get("playerToken").asText();

        // Start
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Host-Token", hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")));

        // Leave during RUNNING -> mark LOST & FINISH (only player)
        mvc.perform(post("/api/rooms/{id}/leave", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", pId)
                        .header("X-Player-Token", pToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("FINISHED")))
                .andExpect(jsonPath("$.players[0].status", is("LOST")))
                .andExpect(jsonPath("$.leaderboard[0].status", is("LOST")));
    }

    @Test
    void leave_with_wrong_token_returns403_apiError() throws Exception {
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();

        String joinJson = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode j = mapper.readTree(joinJson);
        String pId = j.get("playerId").asText();

        mvc.perform(post("/api/rooms/{id}/leave", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", pId)
                        .header("X-Player-Token", "WRONG"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("Invalid player token")));
    }

    @Test
    void leave_unknown_player_returns404_apiError() throws Exception {
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();

        String randomPlayerId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

        mvc.perform(post("/api/rooms/{id}/leave", roomId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Player-Id", randomPlayerId)
                        .header("X-Player-Token", "anything"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Player not found")));
    }

    @Test
    void promote_host_endpoint_rotates_token_and_allows_start() throws Exception {
        // create room
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String oldToken = create.get("hostToken").asText();

        // join a player who will claim host
        String joinJson = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode j = mapper.readTree(joinJson);
        String pId = j.get("playerId").asText();
        String pToken = j.get("playerToken").asText();

        // claim host
        String promoteJson = mvc.perform(post("/api/rooms/{id}/promote-host", roomId)
                        .header("X-Player-Id", pId)
                        .header("X-Player-Token", pToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(roomId)))
                .andExpect(jsonPath("$.hostToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        String newToken = mapper.readTree(promoteJson).get("hostToken").asText();
        assertNotEquals(oldToken, newToken);

        // old host token forbidden
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .header("X-Host-Token", oldToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // new host token starts
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .header("X-Host-Token", newToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")));
    }

    @Test
    void assign_host_endpoint_lets_current_host_pick_player() throws Exception {
        // create room
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String oldToken = create.get("hostToken").asText();

        // join two players
        String j1 = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        String p1 = mapper.readTree(j1).get("playerId").asText();

        String j2 = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        String p2 = mapper.readTree(j2).get("playerId").asText();

        // assign host to p2
        String assignJson = mvc.perform(post("/api/rooms/{id}/assign-host/{target}", roomId, p2)
                        .header("X-Host-Token", oldToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId", is(roomId)))
                .andExpect(jsonPath("$.hostToken", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        String newToken = mapper.readTree(assignJson).get("hostToken").asText();
        assertNotEquals(oldToken, newToken);

        // old token now forbidden
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .header("X-Host-Token", oldToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // new token works
        mvc.perform(post("/api/rooms/{id}/start", roomId)
                        .header("X-Host-Token", newToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")));
    }

    @Test
    void host_can_kick_player_in_lobby() throws Exception {
        // create room
        String createJson = mvc.perform(post("/api/rooms").accept(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        JsonNode create = mapper.readTree(createJson);
        String roomId = create.get("roomId").asText();
        String hostToken = create.get("hostToken").asText();

        // join A
        mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // join B -> we'll kick B
        String joinBJson = mvc.perform(post("/api/rooms/{id}/join", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bId = mapper.readTree(joinBJson).get("playerId").asText();

        // kick B
        mvc.perform(post("/api/rooms/{id}/kick/{playerId}", roomId, bId)
                        .header("X-Host-Token", hostToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("WAITING")))
                .andExpect(jsonPath("$.players[*].playerId", not(hasItem(bId))));
    }


    private int historyCountFor(JsonNode roomView, String playerId) {
        for (JsonNode p : roomView.get("players")) {
            if (p.get("playerId").asText().equals(playerId)) {
                return p.get("history").size();
            }
        }
        return -1;
    }

    @TestConfiguration
    static class FixedSecretConfig {
        @Bean @Primary
        SecretCodeGenerator testSecretGenerator(GameProperties props) {
            return () -> new Code(
                    List.of(0, 1, 3, 2),
                    props.codeLength(),
                    props.minDigit(),
                    props.maxDigit(),
                    props.allowDuplicates()
            );
        }
    }
}
