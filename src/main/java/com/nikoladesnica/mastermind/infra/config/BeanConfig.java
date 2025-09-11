package com.nikoladesnica.mastermind.infra.config;

import com.nikoladesnica.mastermind.domain.ports.GameRepository;
import com.nikoladesnica.mastermind.domain.ports.RoomRepository;
import com.nikoladesnica.mastermind.domain.ports.SecretCodeGenerator;

// Accounts & Leaderboard Extension
import com.nikoladesnica.mastermind.domain.ports.AccountRepository;
import com.nikoladesnica.mastermind.domain.ports.SessionRepository;
import com.nikoladesnica.mastermind.domain.ports.LeaderboardRepository;
import com.nikoladesnica.mastermind.domain.service.AccountService;
import com.nikoladesnica.mastermind.infra.repo.InMemoryAccountRepository;
import com.nikoladesnica.mastermind.infra.repo.InMemorySessionRepository;
import com.nikoladesnica.mastermind.infra.repo.InMemoryLeaderboard;

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
@EnableConfigurationProperties({GameProperties.class, LeaderboardProperties.class})
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
    public AccountRepository accountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public SessionRepository sessionRepository() {
        return new InMemorySessionRepository();
    }

    @Bean
    public LeaderboardRepository leaderboardRepository(LeaderboardProperties props) {
        int k = props.topK() > 0 ? props.topK() : 10;
        return new InMemoryLeaderboard(k);
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

    @Bean
    public AccountService accountService(AccountRepository accounts,
                                         SessionRepository sessions,
                                         LeaderboardRepository leaderboard) {
        return new AccountService(accounts, sessions, leaderboard);
    }
}
