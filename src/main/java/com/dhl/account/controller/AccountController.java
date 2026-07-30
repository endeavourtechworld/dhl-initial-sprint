package com.dhl.account.controller;

import com.dhl.account.dto.RegisterRequest;
import com.dhl.account.dto.VerifyRequest;
import com.dhl.account.model.User;
import com.dhl.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow standard calls from client
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        User user = accountService.registerUser(request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful. A verification OTP has been sent to your email.");
        response.put("email", user.getEmail());
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(@Valid @RequestBody VerifyRequest request) {
        accountService.verifyUser(request);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account successfully verified. You can now log in.");
        
        return ResponseEntity.ok(response);
    }
}
