package com.digitalbank.account;

import com.digitalbank.account.domain.Account;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.account.service.AccountService;
import com.digitalbank.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    @Test
    void shouldCreateAccountWithCorrectData() {
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account result = accountService.createAccount("Maria", new BigDecimal("500.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Maria");
        assertThat(saved.getBalance()).isEqualByComparingTo("500.00");
        assertThat(saved.getId()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void shouldRejectNegativeInitialBalance() {
        assertThatThrownBy(() -> accountService.createAccount("Maria", new BigDecimal("-1.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Saldo inicial não pode ser negativo");
    }

    @Test
    void shouldReturnAccountWhenFound() {
        UUID id = UUID.randomUUID();
        Account account = new Account("João", new BigDecimal("200.00"));
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        Account result = accountService.getAccount(id);

        assertThat(result.getName()).isEqualTo("João");
        assertThat(result.getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(id))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Conta não encontrada para o id");
    }
}
