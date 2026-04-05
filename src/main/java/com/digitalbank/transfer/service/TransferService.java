package com.digitalbank.transfer.service;

import com.digitalbank.account.domain.Account;
import com.digitalbank.account.repository.AccountRepository;
import com.digitalbank.shared.ConflictException;
import com.digitalbank.shared.NotFoundException;
import com.digitalbank.transfer.domain.Statement;
import com.digitalbank.transfer.domain.StatementType;
import com.digitalbank.transfer.domain.Transfer;
import com.digitalbank.transfer.domain.TransferFundsCompletedEvent;
import com.digitalbank.transfer.repository.StatementRepository;
import com.digitalbank.transfer.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private static final Logger LOG = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final StatementRepository statementRepository;
    private final KafkaTemplate<String, TransferFundsCompletedEvent> kafkaTemplate;

    public TransferService(
            AccountRepository accountRepository,
            TransferRepository transferRepository,
            StatementRepository statementRepository,
            KafkaTemplate<String, TransferFundsCompletedEvent> kafkaTemplate
    ) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.statementRepository = statementRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public record TransferResult(Transfer transfer, boolean created) {}

    @Transactional(timeout = 5)
    public TransferResult transferFunds(UUID transferId, UUID sourceId, UUID targetId, BigDecimal amount) {
        if (transferRepository.existsById(transferId)) {
            return handleIdempotentTransfer(transferId, sourceId, targetId, amount);
        }

        LOG.info("method=[TransferService.transferFunds] - Iniciando transferência: transferId={}, de={}, para={}, valor={}", transferId, sourceId, targetId, amount);

        List<Account> accounts = accountRepository.findByIdsForUpdate(List.of(sourceId, targetId));

        Account source = accounts.stream()
            .filter(account -> account.getId().equals(sourceId))
            .findFirst()
            .orElseThrow(() ->
                new NotFoundException("Conta origem não encontrada para o id: " + sourceId)
            );

        Account target = accounts.stream()
            .filter(account -> account.getId().equals(targetId))
            .findFirst()
            .orElseThrow(() ->
                new NotFoundException("Conta destino não encontrada para o id: " + targetId)
            );

        source.debit(amount);
        target.credit(amount);

        Transfer transfer = transferRepository.save(
            new Transfer(transferId, sourceId, targetId, amount)
        );
        statementRepository.save(
            new Statement(sourceId, transferId, StatementType.DEBIT, amount)
        );
        statementRepository.save(
            new Statement(targetId, transferId, StatementType.CREDIT, amount)
        );

        kafkaTemplate.send(
            "transfer-funds-completed",
            new TransferFundsCompletedEvent(transferId, sourceId, targetId, amount)
        );

        LOG.info("method=[TransferService.transferFunds] - Transferência concluída: transferId={}", transferId);

        return new TransferResult(transfer, true);
    }

    private TransferResult handleIdempotentTransfer(UUID transferId, UUID sourceId, UUID targetId, BigDecimal amount) {
        Transfer existing = transferRepository.findById(transferId).orElseThrow();
        boolean sameData = existing.getSourceAccountId().equals(sourceId)
            && existing.getTargetAccountId().equals(targetId)
            && existing.getAmount().compareTo(amount) == 0;
        if (!sameData) {
            throw new ConflictException(
                "Transferência com id=" + transferId + " já existe com dados diferentes"
            );
        }
        LOG.info("method=[TransferService.transferFunds] - Transferência duplicada ignorada: transferId={}", transferId);
        return new TransferResult(existing, false);
    }

}
