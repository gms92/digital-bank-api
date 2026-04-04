package com.digitalbank.transfer;

import com.digitalbank.IntegrationTestBase;
import com.digitalbank.account.domain.Account;
import com.digitalbank.transfer.service.TransferService;
import com.digitalbank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class PessimisticLockTest extends IntegrationTestBase {

    @Autowired TransferService transferService;
    @Autowired AccountRepository accountRepository;

    @Test
    void shouldMaintainConsistentBalanceUnderConcurrentTransfers() throws Exception {
        Account accountA = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        Account accountB = accountRepository.save(new Account("Bob", new BigDecimal("1000.00")));

        UUID transferAtoB = UUID.randomUUID();
        UUID transferBtoA = UUID.randomUUID();

        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                transferService.transferFunds(transferAtoB, accountA.getId(), accountB.getId(), new BigDecimal("100.00"));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }));

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                transferService.transferFunds(transferBtoA, accountB.getId(), accountA.getId(), new BigDecimal("100.00"));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }));

        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        Account finalAccountA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account finalAccountB = accountRepository.findById(accountB.getId()).orElseThrow();

        BigDecimal totalBalance = finalAccountA.getBalance().add(finalAccountB.getBalance());
        assertThat(totalBalance).isEqualByComparingTo("2000.00");
    }
}
