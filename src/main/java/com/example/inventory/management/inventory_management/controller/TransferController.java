package com.example.inventory.management.inventory_management.controller;

import com.example.inventory.management.inventory_management.constant.Constants;
import com.example.inventory.management.inventory_management.model.request.TransferRequest;
import com.example.inventory.management.inventory_management.model.response.TransferResponse;
import com.example.inventory.management.inventory_management.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping(Constants.PATH_TRANSFER)
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        TransferResponse response = transferService.processTransfer(
                request.getFromCurrency(),
                request.getToCurrency(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }
}
