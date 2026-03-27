package com.pbm5.bugtracker.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pbm5.bugtracker.entity.PasswordResetToken;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.repository.PasswordResetTokenRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.service.ResendEmailNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResendEmailNotificationService emailService;

    public void sendResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate simple token
        String token = UUID.randomUUID().toString();

        // Save token (1 hour expiry)
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Send simple email
        String resetUrl = "http://localhost:5173/api/bugtracker/v1/auth/reset-password?token=" + token;
        String htmlContent = "<h1>Reset Password</h1><p>Click <a href='" + resetUrl
                + "'>here</a> to reset your password.</p>";

        emailService.sendNotificationEmail(email, "Reset Password", htmlContent, "Reset your password");
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired or already used");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
