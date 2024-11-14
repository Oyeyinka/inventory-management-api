package com.example.inventory.management.inventory_management.controller;

import com.example.inventory.management.inventory_management.constant.Constants;
import com.example.inventory.management.inventory_management.model.request.FXRateRequest;
import com.example.inventory.management.inventory_management.model.response.Response;
import com.example.inventory.management.inventory_management.service.LiquidityPoolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class FXRateController {

    private final LiquidityPoolService liquidityPoolService;

    public FXRateController(LiquidityPoolService liquidityPoolService) {
        this.liquidityPoolService = liquidityPoolService;
    }

    @PostMapping(Constants.PATH_FX_RATE)
    public ResponseEntity<Response<String>> updateFxRate(@RequestBody FXRateRequest request) {
        return liquidityPoolService.updateFXRate(
                request.getPair(),
                request.getRate(),
                request.getTimestamp()
        );
    }
}

