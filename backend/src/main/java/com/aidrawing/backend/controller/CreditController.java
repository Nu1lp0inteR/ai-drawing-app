package com.aidrawing.backend.controller;

import com.aidrawing.backend.service.CreditService;
import com.aidrawing.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/credits")
public class CreditController {

    private static final Logger logger = LoggerFactory.getLogger(CreditController.class);

    private final CreditService creditService;
    private final JwtService jwtService;

    @Autowired
    public CreditController(CreditService creditService, JwtService jwtService) {
        this.creditService = creditService;
        this.jwtService = jwtService;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        String userId = jwtService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        try {
            Map<String, Object> info = creditService.getBalanceInfo(userId);
            return ResponseEntity.ok(info);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn() {
        String userId = jwtService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        try {
            Map<String, Object> result = creditService.dailySignIn(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = jwtService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "请先登录"));
        }
        try {
            Map<String, Object> result = creditService.getTransactionHistory(userId, page, size);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
