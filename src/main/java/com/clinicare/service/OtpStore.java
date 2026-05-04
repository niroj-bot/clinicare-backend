package com.clinicare.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

    private record OtpEntry(String code, LocalDateTime expiresAt) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code) {
        store.put(email.toLowerCase(), new OtpEntry(code, LocalDateTime.now().plusMinutes(10)));
    }

    public boolean verify(String email, String code) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        if (entry.code().equals(code)) {
            store.remove(email.toLowerCase());
            return true;
        }
        return false;
    }

    public boolean exists(String email) {
        OtpEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return true;
    }
}
