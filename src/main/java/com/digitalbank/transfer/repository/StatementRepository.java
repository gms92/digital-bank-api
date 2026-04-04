package com.digitalbank.transfer.repository;

import com.digitalbank.transfer.domain.Statement;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatementRepository extends JpaRepository<Statement, UUID> {

    Page<Statement> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
