package com.nikoladesnica.mastermind.infra.config;

import com.nikoladesnica.mastermind.domain.ports.GameRepository;
import com.nikoladesnica.mastermind.domain.ports.RoomRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;
import com.nikoladesnica.mastermind.domain.service.GameService;
import com.nikoladesnica.mastermind.domain.service.GuessEvaluator;
import com.nikoladesnica.mastermind.domain.service.RoomService;
import com.nikoladesnica.mastermind.infra.generator.LocalCodeGenerator;
import com.nikoladesnica.mastermind.infra.generator.RandomOrgCodeGenerator;
import com.nikoladesnica.mastermind.infra.repo.InMemoryGameRepository;
import com.nikoladesnica.mastermind.infra.repo.InMemoryRoomRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GameProperties.class)
public class BeanConfig {

    @Bean
    public GameRepository gameRepository() {
        return new InMemoryGameRepository();
    }

    @Bean
    public RoomRepository roomRepository() {
        return new InMemoryRoomRepository();
    }

    @Bean
    public SecretCodeGenerator secretCodeGenerator(GameProperties props) {
        if (props.useRandomOrg()) {
            return new RandomOrgCodeGenerator(props);
        }
        return new LocalCodeGenerator(props);
    }

    @Bean
    public GuessEvaluator guessEvaluator() {
        return new GuessEvaluator();
    }

    @Bean
    public GameService gameService(GameRepository repo,
                                   SecretCodeGenerator gen,
                                   GuessEvaluator eval,
                                   GameProperties props) {
        return new GameService(repo, gen, eval, props);
    }

    @Bean
    public RoomService roomService(RoomRepository rooms,
                                   SecretCodeGenerator gen,
                                   GuessEvaluator eval,
                                   GameProperties props) {
        return new RoomService(rooms, gen, eval, props);
    }
}
