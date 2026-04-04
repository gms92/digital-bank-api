package com.digitalbank.transfer;

import com.digitalbank.account.domain.Account;
import com.digitalbank.transfer.service.TransferService;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.transfer.domain.Statement;
import com.digitalbank.transfer.domain.StatementType;
import com.digitalbank.transfer.domain.Transfer;
import com.digitalbank.transfer.domain.TransferFundsCompletedEvent;
import com.digitalbank.transfer.repository.StatementRepository;
import com.digitalbank.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock TransferRepository transferRepository;
    @Mock StatementRepository statementRepository;
    @Mock KafkaTemplate<String, TransferFundsCompletedEvent> kafkaTemplate;

    @InjectMocks TransferService transferService;

    UUID sourceId;
    UUID targetId;
    Account source;
    Account target;

    @BeforeEach
    void setUp() {
        sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        targetId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        source = Account.builder().id(sourceId).name("Alice")
            .balance(new BigDecimal("1000.00")).createdAt(LocalDateTime.now()).build();
        target = Account.builder().id(targetId).name("Bob")
            .balance(new BigDecimal("500.00")).createdAt(LocalDateTime.now()).build();

        lenient().when(accountRepository.findByIdsForUpdate(anyList()))
            .thenReturn(List.of(source, target));
        lenient().when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(statementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldTransferAmountBetweenAccounts() {
        UUID transferId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");

        transferService.transferFunds(transferId, sourceId, targetId, amount);

        assertThat(source.getBalance()).isEqualByComparingTo("800.00");
        assertThat(target.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void shouldSaveTransferAndTwoStatements() {
        UUID transferId = UUID.randomUUID();

        transferService.transferFunds(transferId, sourceId, targetId, new BigDecimal("100.00"));

        verify(transferRepository).save(any(Transfer.class));

        ArgumentCaptor<Statement> captor = ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository, times(2)).save(captor.capture());
        List<Statement> statements = captor.getAllValues();
        assertThat(statements).hasSize(2);
    }

    @Test
    void shouldPublishKafkaEventAfterTransfer() {
        UUID transferId = UUID.randomUUID();

        transferService.transferFunds(transferId, sourceId, targetId, new BigDecimal("50.00"));

        verify(kafkaTemplate).send(eq("transfer-funds-completed"), any(TransferFundsCompletedEvent.class));
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        when(accountRepository.findByIdsForUpdate(anyList()))
            .thenReturn(List.of(source, target));

        assertThatThrownBy(() ->
            transferService.transferFunds(UUID.randomUUID(), sourceId, targetId, new BigDecimal("9999.00"))
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    void shouldIgnoreDuplicateTransferId() {
        UUID transferId = UUID.randomUUID();
        when(transferRepository.existsById(transferId)).thenReturn(true);
        when(transferRepository.findById(transferId))
            .thenReturn(Optional.of(new Transfer(transferId, sourceId, targetId, new BigDecimal("100.00"))));

        transferService.transferFunds(transferId, sourceId, targetId, new BigDecimal("100.00"));

        verify(accountRepository, never()).findByIdsForUpdate(anyList());
        verify(transferRepository, never()).save(any());
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

        Page<Statement> page1 = transferService.getStatement(accountId, firstPage);
        Page<Statement> page2 = transferService.getStatement(accountId, secondPage);

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
