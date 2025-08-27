package com.nikoladesnica.mastermind.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikoladesnica.mastermind.domain.model.Code;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.infra.config.GameProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        // Ensure we don't hit random.org in tests
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
class GameControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void startGame_thenGuess_thenGet_snapshotIsConsistent() throws Exception {
        // Start
        String startJson = mvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", not(emptyString())))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.attemptsLeft", is(10)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode start = mapper.readTree(startJson);
        String gameId = start.get("gameId").asText();

        // First guess (wrong but valid)
        mvc.perform(post("/api/games/{id}/guesses", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                             {"digits":[0,1,2,3]}
                             """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(gameId)))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.historyCount", is(1)))
                .andExpect(jsonPath("$.attemptsLeft", is(9)))
                .andExpect(jsonPath("$.canGuess", is(true)))
                .andExpect(jsonPath("$.message").doesNotExist());

        // Winning guess (matches fixed generator below: [0,1,3,2])
        mvc.perform(post("/api/games/{id}/guesses", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                             {"digits":[0,1,3,2]}
                             """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WON")))
                .andExpect(jsonPath("$.historyCount", is(2)))
                .andExpect(jsonPath("$.canGuess", is(false)))
                .andExpect(jsonPath("$.message", containsString("Game finished")));

        // After win, state is frozen (a third guess is ignored)
        mvc.perform(post("/api/games/{id}/guesses", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                             {"digits":[0,1,3,2]}
                             """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WON")))
                .andExpect(jsonPath("$.historyCount", is(2)))
                .andExpect(jsonPath("$.canGuess", is(false)))
                .andExpect(jsonPath("$.message", containsString("Game finished")));

        // GET snapshot matches
        mvc.perform(get("/api/games/{id}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(gameId)))
                .andExpect(jsonPath("$.historyCount", is(2)))
                .andExpect(jsonPath("$.canGuess", is(false)))
                .andExpect(jsonPath("$.message", containsString("Game finished")));
    }

    @Test
    void invalidGuessLength_returns400() throws Exception {
        String startJson = mvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String gameId = mapper.readTree(startJson).get("gameId").asText();

        // only 3 digits -> violates @Size(min=4,max=4)
        mvc.perform(post("/api/games/{id}/guesses", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"digits":[0,1,2]}
                     """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidGuessRange_returns400_withApiError() throws Exception {
        String startJson = mvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String gameId = mapper.readTree(startJson).get("gameId").asText();

        mvc.perform(post("/api/games/{id}/guesses", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                     {"digits":[0,1,2,9]}
                     """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("between 0 and 7")))
                .andExpect(jsonPath("$.path", is("/api/games/" + gameId + "/guesses")));
    }

    @Test
    void gameNotFound_returns404_withApiError() throws Exception {
        String fakeId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        mvc.perform(get("/api/games/{id}", fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is("Game not found")))
                .andExpect(jsonPath("$.path", is("/api/games/" + fakeId)));
    }

    @TestConfiguration
    static class FixedSecretConfig {
        // Override the generator with a deterministic secret for tests
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
