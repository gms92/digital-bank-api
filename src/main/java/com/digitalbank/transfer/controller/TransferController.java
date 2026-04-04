package com.digitalbank.transfer.controller;

import com.digitalbank.transfer.dto.TransferRequest;
import com.digitalbank.transfer.dto.TransferResponse;
import com.digitalbank.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Transfers", description = "Transferências entre contas")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Executa uma transferência de valores entre contas")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        return TransferResponse.from(
            transferService.transferFunds(
                request.transferId(),
                request.sourceAccountId(),
                request.targetAccountId(),
                request.amount()
            )
        );
    }
}
