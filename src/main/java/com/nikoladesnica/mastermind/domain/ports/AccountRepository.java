package com.nikoladesnica.mastermind.domain.ports;

import com.nikoladesnica.mastermind.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
    Optional<Account> findByUsername(String username);
    Account save(Account account);
    void incrementWins(UUID accountId);
    void incrementLosses(UUID accountId);
    List<Account> all();
}
