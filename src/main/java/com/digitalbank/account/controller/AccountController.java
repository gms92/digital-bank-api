package com.digitalbank.account.controller;

import com.digitalbank.account.dto.AccountRequest;
import com.digitalbank.account.dto.AccountResponse;
import com.digitalbank.account.service.AccountService;
import com.digitalbank.transfer.dto.StatementResponse;
import com.digitalbank.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Gestão de contas")
public class AccountController {

    private final AccountService accountService;
    private final TransferService transferService;

    public AccountController(AccountService accountService, TransferService transferService) {
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma nova conta")
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return AccountResponse.from(
            accountService.createAccount(request.name(), request.initialBalance())
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma conta")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @GetMapping("/{id}/statements")
    @Operation(summary = "Consulta extrato de uma conta")
    public Page<StatementResponse> getStatements(
        @PathVariable UUID id,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return transferService.getStatement(id, pageable)
            .map(StatementResponse::from);
    }
}
