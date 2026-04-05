package com.digitalbank.transfer.controller;

import com.digitalbank.transfer.dto.TransferRequest;
import com.digitalbank.transfer.dto.TransferResponse;
import com.digitalbank.transfer.service.TransferService;
import com.digitalbank.transfer.service.TransferService.TransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Transfers", description = "Transferências entre contas")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfers")
    @Operation(summary = "Executa uma transferência de valores entre contas")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResult result = transferService.transferFunds(
            request.transferId(),
            request.sourceAccountId(),
            request.targetAccountId(),
            request.amount()
        );
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TransferResponse.from(result.transfer()));
    }
}
