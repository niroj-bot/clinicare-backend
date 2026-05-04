package com.clinicare.controller;

import com.clinicare.model.InsuranceCard;
import com.clinicare.model.User;
import com.clinicare.repository.InsuranceRepository;
import com.clinicare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository      userRepo;
    private final InsuranceRepository insuranceRepo;

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication auth) {
        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id",    user.getId());
        profile.put("name",  user.getName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        insuranceRepo.findByUserId(user.getId()).ifPresent(ins -> {
            Map<String, Object> insMap = new LinkedHashMap<>();
            insMap.put("id",            ins.getId());
            insMap.put("insuranceType", ins.getInsuranceType());
            insMap.put("insuranceName", ins.getInsuranceName());
            insMap.put("policyNumber",  ins.getPolicyNumber());
            insMap.put("holderName",    ins.getHolderName());
            profile.put("insurance", insMap);
        });
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (req.name()  != null) user.setName(req.name());
        if (req.phone() != null) user.setPhone(req.phone());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @PutMapping("/insurance")
    public ResponseEntity<?> saveInsurance(@RequestBody InsuranceRequest req, Authentication auth) {
        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        InsuranceCard card = insuranceRepo.findByUserId(user.getId())
                .orElse(InsuranceCard.builder().user(user).build());
        card.setInsuranceType(req.insuranceType());
        card.setInsuranceName(req.insuranceName());
        card.setPolicyNumber(req.policyNumber());
        card.setHolderName(req.holderName());
        insuranceRepo.save(card);
        return ResponseEntity.ok(Map.of("saved", true));
    }

    record UpdateProfileRequest(String name, String phone) {}
    record InsuranceRequest(
        String insuranceType, String insuranceName,
        String policyNumber,  String holderName
    ) {}
}
