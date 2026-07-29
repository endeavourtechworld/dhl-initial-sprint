package com.dhl.account.service;

import com.dhl.account.dto.RegisterRequest;
import com.dhl.account.dto.VerifyRequest;
import com.dhl.account.model.User;
import com.dhl.account.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private RegisterRequest registerRequest;

    @BeforeEach
    public void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("John Doe");
        registerRequest.setEmail("john.doe@dhl-test.com");
        registerRequest.setMobileNumber("+1234567890");
        registerRequest.setPassword("DhlSecurePass1!");
        registerRequest.setConfirmPassword("DhlSecurePass1!");
    }

    @Test
    public void testRegisterUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encrypted_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = accountService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getFullName());
        assertEquals("john.doe@dhl-test.com", result.getEmail());
        assertEquals("encrypted_password", result.getPassword());
        assertFalse(result.isVerified());
        assertNotNull(result.getVerificationCode());
        assertEquals(6, result.getVerificationCode().length());
        assertTrue(result.getVerificationCodeExpiresAt().isAfter(LocalDateTime.now()));

        verify(userRepository, times(1)).existsByEmail(anyString());
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegisterUser_PasswordMismatch_ThrowsException() {
        // Arrange
        registerRequest.setConfirmPassword("DifferentPass2#");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.registerUser(registerRequest);
        });

        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRegisterUser_DuplicateEmail_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.registerUser(registerRequest);
        });

        assertEquals("Email address is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testVerifyUser_Success() {
        // Arrange
        User mockUser = new User("John Doe", "john.doe@dhl-test.com", "+1234567890", "encrypted_pass");
        mockUser.setVerificationCode("123456");
        mockUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

        VerifyRequest verifyRequest = new VerifyRequest();
        verifyRequest.setEmail("john.doe@dhl-test.com");
        verifyRequest.setCode("123456");

        when(userRepository.findByEmail("john.doe@dhl-test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        boolean result = accountService.verifyUser(verifyRequest);

        // Assert
        assertTrue(result);
        assertTrue(mockUser.isVerified());
        assertNull(mockUser.getVerificationCode());
        assertNull(mockUser.getVerificationCodeExpiresAt());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    public void testVerifyUser_InvalidCode_ThrowsException() {
        // Arrange
        User mockUser = new User("John Doe", "john.doe@dhl-test.com", "+1234567890", "encrypted_pass");
        mockUser.setVerificationCode("123456");
        mockUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));

        VerifyRequest verifyRequest = new VerifyRequest();
        verifyRequest.setEmail("john.doe@dhl-test.com");
        verifyRequest.setCode("999999"); // Invalid Code

        when(userRepository.findByEmail("john.doe@dhl-test.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.verifyUser(verifyRequest);
        });

        assertEquals("Invalid verification code", exception.getMessage());
        assertFalse(mockUser.isVerified());
        verify(userRepository, never()).save(mockUser);
    }

    @Test
    public void testVerifyUser_ExpiredCode_ThrowsException() {
        // Arrange
        User mockUser = new User("John Doe", "john.doe@dhl-test.com", "+1234567890", "encrypted_pass");
        mockUser.setVerificationCode("123456");
        mockUser.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired

        VerifyRequest verifyRequest = new VerifyRequest();
        verifyRequest.setEmail("john.doe@dhl-test.com");
        verifyRequest.setCode("123456");

        when(userRepository.findByEmail("john.doe@dhl-test.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.verifyUser(verifyRequest);
        });

        assertEquals("Verification code has expired. Please register again or request a new code.", exception.getMessage());
        assertFalse(mockUser.isVerified());
        verify(userRepository, never()).save(mockUser);
    }
}
