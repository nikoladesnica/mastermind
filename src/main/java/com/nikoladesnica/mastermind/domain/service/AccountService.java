package com.nikoladesnica.mastermind.domain.service;

import com.nikoladesnica.mastermind.domain.errors.BadRequestException;
import com.nikoladesnica.mastermind.domain.errors.NotFoundException;
import com.nikoladesnica.mastermind.domain.model.Account;
import com.nikoladesnica.mastermind.domain.model.Session;
import com.nikoladesnica.mastermind.domain.ports.AccountRepository;
import com.nikoladesnica.mastermind.domain.ports.LeaderboardRepository;
import com.nikoladesnica.mastermind.domain.ports.SessionRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class AccountService {

    private final AccountRepository accounts;
    private final SessionRepository sessions;
    private final LeaderboardRepository leaderboard;

    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32; // bytes
    private static final int ITER = 120_000;

    public AccountService(AccountRepository accounts, SessionRepository sessions, LeaderboardRepository leaderboard) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.leaderboard = leaderboard;
    }

    public UUID createAccount(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username required");
        }
        if (password == null || password.length() < 6) {
            throw new BadRequestException("Password too short");
        }
        if (accounts.findByUsername(username).isPresent()) {
            throw new BadRequestException("Username already exists");
        }
        byte[] salt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(password.toCharArray(), salt, ITER, HASH_LEN);
        Account a = new Account(username, hash, salt, ITER);
        accounts.save(a);
        return a.id();
    }

    public UUID login(String username, String password) {
        Account a = accounts.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        byte[] calc = hash(password.toCharArray(), a.salt(), a.iterations(), HASH_LEN);
        if (!constantTimeEquals(calc, a.passwordHash())) {
            throw new BadRequestException("Invalid credentials");
        }
        a.setLastLoginAt(Instant.now());
        Session s = new Session(a.id());
        sessions.put(s);
        return s.token();
    }

    public UUID accountIdFromSession(UUID sessionToken) {
        if (sessionToken == null) throw new BadRequestException("Session token required");
        return sessions.get(sessionToken)
                .map(Session::accountId)
                .orElseThrow(() -> new BadRequestException("Invalid session"));
    }

    public void recordWin(UUID accountId) {
        accounts.incrementWins(accountId);
        leaderboard.increment(accountId);
    }

    public void recordLoss(UUID accountId) {
        accounts.incrementLosses(accountId);
    }

    private static byte[] hash(char[] pwd, byte[] salt, int iter, int outLen) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pwd, salt, iter, outLen * 8);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }
}
