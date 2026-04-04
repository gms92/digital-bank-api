package com.digitalbank.transfer.repository;

import com.digitalbank.transfer.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> { }
