package com.nikoladesnica.mastermind.api;

import com.nikoladesnica.mastermind.api.dto.CreateAccountRequest;
import com.nikoladesnica.mastermind.api.dto.CreateAccountResponse;
import com.nikoladesnica.mastermind.api.dto.LoginRequest;
import com.nikoladesnica.mastermind.api.dto.LoginResponse;
import com.nikoladesnica.mastermind.domain.ports.AccountRepository;
import com.nikoladesnica.mastermind.domain.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

    private final AccountService accounts;
    private final AccountRepository repo;

    public AccountController(AccountService accounts, AccountRepository repo) {
        this.accounts = accounts;
        this.repo = repo;
    }

    @PostMapping("/accounts")
    public ResponseEntity<CreateAccountResponse> create(@RequestBody CreateAccountRequest req) {
        var id = accounts.createAccount(req.username(), req.password());
        return ResponseEntity.ok(new CreateAccountResponse(id, req.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        var token = accounts.login(req.username(), req.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
