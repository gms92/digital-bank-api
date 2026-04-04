package com.digitalbank.account.service;

import com.digitalbank.account.domain.Account;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.shared.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(String name, BigDecimal initialBalance) {
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Saldo inicial não pode ser negativo");
        }
        Account account = accountRepository.save(new Account(name, initialBalance));
        LOG.info("method=[AccountService.createAccount] - Conta criada: id={}, nome={}", account.getId(), name);
        return account;
    }

    public Account getAccount(UUID id) {
        return accountRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada para o id: " + id));
    }
}
