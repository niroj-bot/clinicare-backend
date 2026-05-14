package com.clinicare.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicare.model.Booking;
import com.clinicare.model.Clinic;
import com.clinicare.model.ClinicService;
import com.clinicare.model.TimeSlot;
import com.clinicare.model.User;
import com.clinicare.repository.BookingRepository;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ClinicServiceRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.repository.UserRepository;
import com.clinicare.service.SlotScheduler;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClinicRepository        clinicRepo;
    private final ClinicServiceRepository serviceRepo;
    private final TimeSlotRepository      slotRepo;
    private final BookingRepository       bookingRepo;
    private final UserRepository          userRepo;
    private final SlotScheduler           slotScheduler;
    private final PasswordEncoder         passwordEncoder;

    // Clinics

    @GetMapping("/clinics")
    public ResponseEntity<?> getAllClinics() {
        List<Clinic> clinics = clinicRepo.findAll();
        List<Long> clinicIds = clinics.stream().map(Clinic::getId).toList();

        // Fetch ALL services and slots
        Map<Long, List<ClinicService>> servicesMap = serviceRepo.findByClinicIdIn(clinicIds)
                .stream().collect(Collectors.groupingBy(s -> s.getClinic().getId()));

        Map<Long, List<TimeSlot>> slotsMap = slotRepo.findByClinicIdInAndIsBookedFalseOrderByDateAscStartTimeAsc(clinicIds)
                .stream().collect(Collectors.groupingBy(s -> s.getClinic().getId()));

        List<Map<String, Object>> result = clinics.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",            c.getId());
            map.put("name",          c.getName());
            map.put("address",       c.getAddress());
            map.put("phone",         c.getPhone());
            map.put("description",   c.getDescription());
            map.put("imageUrl",      c.getImageUrl());
            map.put("latitude",      c.getLatitude());
            map.put("longitude",     c.getLongitude());
            map.put("averageRating", c.getAverageRating());
            map.put("reviewCount",   c.getReviewCount());
            map.put("active",        c.getActive());

            map.put("services", servicesMap.getOrDefault(c.getId(), List.of()).stream().map(s -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("id",              s.getId());
                sm.put("serviceName",     s.getServiceName());
                sm.put("category",        s.getCategory());
                sm.put("price",           s.getPrice());
                sm.put("durationMinutes", s.getDurationMinutes());
                sm.put("available",       s.getAvailable());
                return sm;
            }).toList());

            map.put("timeSlots", slotsMap.getOrDefault(c.getId(), List.of()).stream().map(sl -> {
                Map<String, Object> slm = new LinkedHashMap<>();
                slm.put("id",          sl.getId());
                slm.put("date",        sl.getDate().toString());
                slm.put("startTime",   sl.getStartTime().toString());
                slm.put("endTime",     sl.getEndTime().toString());
                slm.put("isBooked",    sl.getIsBooked());
                slm.put("waitMinutes", sl.getWaitMinutes());
                return slm;
            }).toList());

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/clinics")
    public ResponseEntity<?> createClinic(@RequestBody ClinicRequest req) {
        Clinic clinic = Clinic.builder()
                .name(req.name()).address(req.address()).phone(req.phone())
                .description(req.description()).imageUrl(req.imageUrl())
                .latitude(req.latitude()).longitude(req.longitude())
                .active(true).averageRating(0.0).reviewCount(0)
                .build();
        Clinic saved = clinicRepo.save(clinic);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "name", saved.getName()));
    }

    @PutMapping("/clinics/{id}")
    public ResponseEntity<?> updateClinic(@PathVariable Long id, @RequestBody ClinicRequest req) {
        Clinic clinic = clinicRepo.findById(id).orElseThrow(() -> new RuntimeException("Clinic not found"));
        clinic.setName(req.name());
        clinic.setAddress(req.address());
        clinic.setPhone(req.phone());
        clinic.setDescription(req.description());
        clinic.setLatitude(req.latitude());
        clinic.setLongitude(req.longitude());
        clinicRepo.save(clinic);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @DeleteMapping("/clinics/{id}")
    public ResponseEntity<?> deleteClinic(@PathVariable Long id) {
        clinicRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // Services

    @PostMapping("/clinics/{clinicId}/services")
    public ResponseEntity<?> addService(@PathVariable Long clinicId, @RequestBody ServiceRequest req) {
        Clinic clinic = clinicRepo.findById(clinicId).orElseThrow();
        ClinicService svc = ClinicService.builder()
                .clinic(clinic).serviceName(req.serviceName()).category(req.category())
                .description(req.description()).price(req.price())
                .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 30)
                .available(true)
                .build();
        ClinicService saved = serviceRepo.save(svc);
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    @PutMapping("/services/{serviceId}")
    public ResponseEntity<?> updateService(@PathVariable Long serviceId, @RequestBody ServiceRequest req) {
        ClinicService svc = serviceRepo.findById(serviceId).orElseThrow();
        svc.setServiceName(req.serviceName());
        svc.setCategory(req.category());
        svc.setPrice(req.price());
        if (req.durationMinutes() != null) svc.setDurationMinutes(req.durationMinutes());
        if (req.available() != null) svc.setAvailable(req.available());
        serviceRepo.save(svc);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<?> deleteService(@PathVariable Long serviceId) {
        serviceRepo.deleteById(serviceId);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // Time Slots

    @PostMapping("/clinics/{clinicId}/slots")
    public ResponseEntity<?> addSlot(@PathVariable Long clinicId, @RequestBody SlotRequest req) {
        Clinic clinic = clinicRepo.findById(clinicId).orElseThrow();
        TimeSlot slot = TimeSlot.builder()
                .clinic(clinic)
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .waitMinutes(req.waitMinutes() != null ? req.waitMinutes() : 0)
                .isBooked(false)
                .build();
        TimeSlot saved = slotRepo.save(slot);
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    // Bookings
    @Transactional(readOnly = true)
    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        List<Map<String, Object>> result = bookingRepo.findAll().stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id",          b.getId());
                    map.put("bookingRef",  b.getBookingRef());
                    map.put("guestOrUser", b.getUser() != null ? b.getUser().getName() : b.getGuestName());
                    map.put("clinicName",  b.getClinic().getName());
                    map.put("service",     b.getService().getServiceName());
                    map.put("date",        b.getTimeSlot().getDate().toString());
                    map.put("startTime",   b.getTimeSlot().getStartTime().toString());
                    map.put("status",      b.getStatus().name());
                    return map;
                }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/clinics/{clinicId}/bookings")
    public ResponseEntity<?> getClinicBookings(@PathVariable Long clinicId) {
        List<Map<String, Object>> result = bookingRepo.findByClinicId(clinicId).stream()
                .map(b -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id",          b.getId());
                    map.put("bookingRef",  b.getBookingRef());
                    map.put("guestOrUser", b.getUser() != null ? b.getUser().getName() : b.getGuestName());
                    map.put("service",     b.getService().getServiceName());
                    map.put("date",        b.getTimeSlot().getDate().toString());
                    map.put("startTime",   b.getTimeSlot().getStartTime().toString());
                    map.put("status",      b.getStatus().name());
                    return map;
                }).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Booking booking = bookingRepo.findById(id).orElseThrow();
        booking.setStatus(Booking.Status.valueOf(body.get("status")));
        bookingRepo.save(booking);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    // Stats

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalClinics",   clinicRepo.count());
        stats.put("totalBookings",  bookingRepo.count());
        stats.put("totalServices",  serviceRepo.count());
        stats.put("totalSlots",     slotRepo.count());
        return ResponseEntity.ok(stats);
    }

    // Request records 

    record ClinicRequest(
        String name, String address, String phone,
        String description, String imageUrl,
        Double latitude, Double longitude
    ) {}

    record ServiceRequest(
        String serviceName, String category, String description,
        BigDecimal price, Integer durationMinutes, Boolean available
    ) {}

    record SlotRequest(
        LocalDate date, LocalTime startTime,
        LocalTime endTime, Integer waitMinutes
    ) {}

    // Create clinic account
    @PostMapping("/clinics/{id}/account")
    public ResponseEntity<?> createClinicAccount(
            @PathVariable Long id,
            @RequestBody CreateAccountRequest req) {

        Clinic clinic = clinicRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        // Check email not already taken
        if (userRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }

        // Check clinic doesn't already have an account
        boolean alreadyHasAccount = userRepo.findAll().stream()
                .anyMatch(u -> u.getRole() == User.Role.CLINIC
                        && u.getManagedClinic() != null
                        && u.getManagedClinic().getId().equals(id));
        if (alreadyHasAccount) {
            return ResponseEntity.badRequest().body(Map.of("error", "This clinic already has an account"));
        }

        User user = User.builder()
                .name(clinic.getName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(User.Role.CLINIC)
                .managedClinic(clinic)
                .build();
        userRepo.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Account created for " + clinic.getName(),
            "email",   req.email()
        ));
    }

    // Get clinic account info
    @GetMapping("/clinics/{id}/account")
    public ResponseEntity<?> getClinicAccount(@PathVariable Long id) {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole() == User.Role.CLINIC
                        && u.getManagedClinic() != null
                        && u.getManagedClinic().getId().equals(id))
                .findFirst()
                .map(u -> ResponseEntity.ok(Map.of(
                    "hasAccount", true,
                    "email",      u.getEmail(),
                    "name",       u.getName()
                )))
                .orElse(ResponseEntity.ok(Map.of("hasAccount", false)));
    }

    record CreateAccountRequest(String email, String password) {}


    // Manual slot generation
    @PostMapping("/slots/generate")
    public ResponseEntity<?> triggerSlotGeneration() {
        slotScheduler.generateOnStartup();
        return ResponseEntity.ok(Map.of("message", "Slot generation triggered"));
    }

}