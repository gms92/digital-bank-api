package com.digitalbank.account;

import com.digitalbank.account.domain.Account;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.account.service.AccountService;
import com.digitalbank.shared.NotFoundException;
import com.digitalbank.transfer.domain.Statement;
import com.digitalbank.transfer.domain.StatementType;
import com.digitalbank.transfer.repository.StatementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    StatementRepository statementRepository;

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

    @Test
    void shouldReturnStatementsWithPagination() {
        UUID accountId = UUID.randomUUID();
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        UUID transferId1 = UUID.randomUUID();
        UUID transferId2 = UUID.randomUUID();
        UUID transferId3 = UUID.randomUUID();

        Statement statement1 = new Statement(accountId, transferId1, StatementType.DEBIT, new BigDecimal("10.00"));
        Statement statement2 = new Statement(accountId, transferId2, StatementType.CREDIT, new BigDecimal("20.00"));
        Statement statement3 = new Statement(accountId, transferId3, StatementType.DEBIT, new BigDecimal("30.00"));

        when(statementRepository.findByAccountIdOrderByCreatedAtDesc(accountId, firstPage))
            .thenReturn(new PageImpl<>(List.of(statement1, statement2), firstPage, 3));
        when(statementRepository.findByAccountIdOrderByCreatedAtDesc(accountId, secondPage))
            .thenReturn(new PageImpl<>(List.of(statement3), secondPage, 3));

        Page<Statement> page1 = accountService.getStatement(accountId, firstPage);
        Page<Statement> page2 = accountService.getStatement(accountId, secondPage);

        assertThat(page1.getContent()).containsExactly(statement1, statement2);
        assertThat(page1.getContent().get(0).getType()).isEqualTo(StatementType.DEBIT);
        assertThat(page1.getContent().get(1).getType()).isEqualTo(StatementType.CREDIT);
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.hasNext()).isTrue();

        assertThat(page2.getContent()).containsExactly(statement3);
        assertThat(page2.getContent().get(0).getType()).isEqualTo(StatementType.DEBIT);
        assertThat(page2.hasNext()).isFalse();
    }
}
