package com.clinicare.controller;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicare.config.JwtUtil;
import com.clinicare.email.EmailService;
import com.clinicare.model.Booking;
import com.clinicare.model.User;
import com.clinicare.repository.BookingRepository;
import com.clinicare.repository.UserRepository;
import com.clinicare.service.OtpStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository    userRepo;
    private final BookingRepository bookingRepo;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;
    private final EmailService      emailService;
    private final OtpStore          otpStore;

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(999999));
    }

    // Send OTP to email
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        String otp = generateOtp();
        otpStore.save(req.email(), otp);
        emailService.sendOtp(req.email(), req.name(), otp);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to " + req.email()));
    }

    // Verify OTP + complete registration
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        if (!otpStore.verify(req.email(), req.otp())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification code"));
        }
        User user = User.builder()
                .name(req.name()).email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phone(req.phone()).role(User.Role.USER)
                .build();
        userRepo.save(user);

        // Merge guest bookings
        List<Booking> guestBookings = bookingRepo.findByGuestEmail(req.email());
        if (!guestBookings.isEmpty()) {
            guestBookings.forEach(b -> b.setUser(user));
            bookingRepo.saveAll(guestBookings);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
            "token", token, "name", user.getName(),
            "email", user.getEmail(), "role", user.getRole().name(),
            "mergedBookings", guestBookings.size()
        ));
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepo.findByEmail(req.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email or password"));
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
            "token", token, "name", user.getName(),
            "email", user.getEmail(), "role", user.getRole().name()
        ));
    }

    // Forgot password: send OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody EmailRequest req) {
        User user = userRepo.findByEmail(req.email()).orElse(null);
        if (user == null) {
            // Don't reveal if email exists
            return ResponseEntity.ok(Map.of("message", "If that email exists, a code was sent"));
        }
        String otp = generateOtp();
        otpStore.save("reset_" + req.email(), otp);
        emailService.sendPasswordResetOtp(req.email(), user.getName(), otp);
        return ResponseEntity.ok(Map.of("message", "Password reset code sent to " + req.email()));
    }

    // Reset password: verify OTP + set new password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        if (!otpStore.verify("reset_" + req.email(), req.otp())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired code"));
        }
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    // Records
    record SendOtpRequest(String name, @Email String email) {}
    record RegisterRequest(
        @NotBlank String name, @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        String phone, @NotBlank String otp
    ) {}
    record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    record EmailRequest(String email) {}
    record ResetPasswordRequest(String email, String otp, @Size(min = 6) String newPassword) {}
}
