package com.nikoladesnica.mastermind.infra.repo;

import com.nikoladesnica.mastermind.domain.model.Account;
import com.nikoladesnica.mastermind.domain.ports.AccountRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository implements AccountRepository {

    private final Map<UUID, Account> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> idByUsername = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        UUID id = idByUsername.get(username);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public Account save(Account account) {
        byId.put(account.id(), account);
        idByUsername.put(account.username(), account.id());
        return account;
    }

    @Override
    public void incrementWins(UUID accountId) {
        Account a = byId.get(accountId);
        if (a != null) a.incrementWins();
    }

    @Override
    public void incrementLosses(UUID accountId) {
        Account a = byId.get(accountId);
        if (a != null) a.incrementLosses();
    }

    @Override
    public List<Account> all() {
        return List.copyOf(byId.values());
    }
}
