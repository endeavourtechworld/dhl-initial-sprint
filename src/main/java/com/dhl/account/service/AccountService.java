package com.dhl.account.service;

import com.dhl.account.dto.RegisterRequest;
import com.dhl.account.dto.VerifyRequest;
import com.dhl.account.model.User;
import com.dhl.account.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Transactional
    public User registerUser(RegisterRequest request) {
        logger.info("Register attempt initiated for email: {}", request.getEmail());

        // Validate password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            logger.warn("Register attempt failed - passwords do not match for email: {}", request.getEmail());
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Validate unique email
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Register attempt failed - email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email address is already registered");
        }

        // Encrypt password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Generate OTP
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5); // 5-minute validity

        User user = new User(
            request.getFullName(),
            request.getEmail(),
            request.getMobileNumber(),
            hashedPassword
        );
        user.setVerificationCode(otp);
        user.setVerificationCodeExpiresAt(expiresAt);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully. ID: {}, Email: {}. Verification code generated.", savedUser.getId(), savedUser.getEmail());

        // Simulate sending verification email
        sendVerificationEmail(savedUser.getEmail(), savedUser.getFullName(), otp);

        return savedUser;
    }

    @Transactional
    public boolean verifyUser(VerifyRequest request) {
        logger.info("Verification attempt for email: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            logger.warn("Verification failed - user not found: {}", request.getEmail());
            throw new IllegalArgumentException("User with this email does not exist");
        }

        User user = userOpt.get();

        if (user.isVerified()) {
            logger.info("User already verified: {}", request.getEmail());
            return true;
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            logger.warn("Verification failed - invalid OTP code for email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid verification code");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Verification failed - expired OTP code for email: {}", request.getEmail());
            throw new IllegalArgumentException("Verification code has expired. Please register again or request a new code.");
        }

        // Mark user as verified
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        logger.info("Verification succeeded for email: {}. User is now active.", user.getEmail());
        return true;
    }

    private String generateOtp() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendVerificationEmail(String email, String fullName, String code) {
        // Log simulation of email dispatch
        logger.info("------------------------------------------------------------");
        logger.info("SIMULATED EMAIL DISPATCH TO: {}", email);
        logger.info("Recipient Name: {}", fullName);
        logger.info("Subject: Verify Your DHL Account");
        logger.info("Body: Welcome to DHL, {}. Your 6-digit verification code is [ {} ]. Valid for 5 minutes.", fullName, code);
        logger.info("------------------------------------------------------------");
    }
}
