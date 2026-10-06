package com.olukotun.payments.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
                "service","payment-transfer-api",
                "status","running"
        );
    }
}
