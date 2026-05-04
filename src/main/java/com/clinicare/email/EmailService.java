package com.clinicare.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${app.sendgrid.api-key:}")
    private String apiKey;

    @Value("${app.sendgrid.from-email:noreply@clinicare.com}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    // Send any email via SendGrid
    private void send(String toEmail, String toName, String subject, String html) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("SendGrid not configured — skipping email to {}", toEmail);
            return;
        }
        Map<String, Object> body = Map.of(
            "personalizations", List.of(Map.of(
                "to", List.of(Map.of("email", toEmail, "name", toName))
            )),
            "from",    Map.of("email", fromEmail, "name", "ClinICare"),
            "subject", subject,
            "content", List.of(Map.of("type", "text/html", "value", html))
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        try {
            restTemplate.exchange(
                "https://api.sendgrid.com/v3/mail/send",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
            );
            log.info("Email sent to {}: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    // 6-digit OTP verification email
    public void sendOtp(String toEmail, String toName, String otp) {
        String html = """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto;">
              <div style="background:#1a6b4a;padding:24px;border-radius:12px 12px 0 0;">
                <h1 style="color:white;margin:0;font-size:20px;">ClinICare</h1>
                <p style="color:rgba(255,255,255,.8);margin:4px 0 0;">Email Verification</p>
              </div>
              <div style="background:#f7faf8;padding:28px;border-radius:0 0 12px 12px;">
                <p>Hi <strong>%s</strong>,</p>
                <p>Your verification code is:</p>
                <div style="text-align:center;margin:24px 0;">
                  <span style="font-size:36px;font-weight:700;letter-spacing:10px;color:#1a6b4a;background:white;padding:16px 24px;border-radius:12px;border:2px solid #1a6b4a;">%s</span>
                </div>
                <p style="color:#8a9490;font-size:13px;">This code expires in 10 minutes. Do not share it with anyone.</p>
              </div>
            </div>
            """.formatted(toName, otp);
        send(toEmail, toName, "Your ClinICare verification code: " + otp, html);
    }

    // Password reset OTP
    public void sendPasswordResetOtp(String toEmail, String toName, String otp) {
        String html = """
            <div style="font-family:sans-serif;max-width:480px;margin:0 auto;">
              <div style="background:#1a6b4a;padding:24px;border-radius:12px 12px 0 0;">
                <h1 style="color:white;margin:0;font-size:20px;">ClinICare</h1>
                <p style="color:rgba(255,255,255,.8);margin:4px 0 0;">Password Reset</p>
              </div>
              <div style="background:#f7faf8;padding:28px;border-radius:0 0 12px 12px;">
                <p>Hi <strong>%s</strong>,</p>
                <p>Use this code to reset your password:</p>
                <div style="text-align:center;margin:24px 0;">
                  <span style="font-size:36px;font-weight:700;letter-spacing:10px;color:#d63e3e;background:white;padding:16px 24px;border-radius:12px;border:2px solid #d63e3e;">%s</span>
                </div>
                <p style="color:#8a9490;font-size:13px;">This code expires in 10 minutes. If you didn't request this, ignore this email.</p>
              </div>
            </div>
            """.formatted(toName, otp);
        send(toEmail, toName, "ClinICare password reset code", html);
    }

    // Booking confirmation email
    public void sendBookingConfirmation(String toEmail, String toName,
            String bookingRef, String clinicName, String serviceName, String date, String time) {
        String html = """
            <div style="font-family:sans-serif;max-width:500px;margin:0 auto;">
              <div style="background:#1a6b4a;padding:24px;border-radius:12px 12px 0 0;">
                <h1 style="color:white;margin:0;font-size:20px;">ClinICare</h1>
                <p style="color:rgba(255,255,255,.8);margin:4px 0 0;">Booking Confirmed</p>
              </div>
              <div style="background:#f7faf8;padding:24px;border-radius:0 0 12px 12px;">
                <p>Hi <strong>%s</strong>, your appointment is confirmed!</p>
                <div style="background:white;border-radius:8px;padding:16px;margin:16px 0;border:1px solid #e4ece8;">
                  <p style="margin:0 0 8px;"><strong>Ref:</strong> <code>%s</code></p>
                  <p style="margin:0 0 8px;"><strong>Clinic:</strong> %s</p>
                  <p style="margin:0 0 8px;"><strong>Service:</strong> %s</p>
                  <p style="margin:0 0 8px;"><strong>Date:</strong> %s</p>
                  <p style="margin:0;"><strong>Time:</strong> %s</p>
                </div>
                <p style="color:#8a9490;font-size:13px;">Please arrive 10 minutes early.</p>
              </div>
            </div>
            """.formatted(toName, bookingRef, clinicName, serviceName, date, time);
        send(toEmail, toName, "Booking Confirmed — " + bookingRef, html);
    }
}
