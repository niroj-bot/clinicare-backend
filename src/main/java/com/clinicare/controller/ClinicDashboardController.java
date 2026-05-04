package com.clinicare.controller;

import com.clinicare.service.ClinicDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/clinic-dashboard")
@RequiredArgsConstructor
public class ClinicDashboardController {

    private final ClinicDashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getDashboard(auth.getName()));
    }

    @GetMapping("/info")
    public ResponseEntity<?> getInfo(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getClinicInfo(auth.getName()));
    }

    @PutMapping("/info")
    public ResponseEntity<?> updateInfo(@RequestBody ClinicInfoRequest req, Authentication auth) {
        dashboardService.updateClinicInfo(auth.getName(), req.name(), req.address(), req.phone(), req.description());
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> bookings(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return ResponseEntity.ok(dashboardService.getBookings(auth.getName(), date, status));
    }

    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        dashboardService.updateBookingStatus(id, body.get("status"));
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @GetMapping("/services")
    public ResponseEntity<?> services(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getServices(auth.getName()));
    }

    @PostMapping("/services")
    public ResponseEntity<?> addService(@RequestBody ServiceRequest req, Authentication auth) {
        dashboardService.addService(auth.getName(), req.serviceName(), req.category(),
                req.description(), req.price(), req.durationMinutes());
        return ResponseEntity.ok(Map.of("created", true));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody ServiceRequest req) {
        dashboardService.updateService(id, req.serviceName(), req.category(),
                req.price(), req.durationMinutes(), req.available());
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @GetMapping("/slots")
    public ResponseEntity<?> slots(@RequestParam(required = false) String date, Authentication auth) {
        return ResponseEntity.ok(dashboardService.getSlots(auth.getName(), date));
    }

    @GetMapping("/slots/availability")
    public ResponseEntity<?> slotAvailability(
            @RequestParam(required = false) String date, Authentication auth) {
        return ResponseEntity.ok(dashboardService.getSlotAvailability(auth.getName(), date));
    }

    @PostMapping("/slots")
    public ResponseEntity<?> addSlot(@RequestBody SlotRequest req, Authentication auth) {
        dashboardService.addSlot(auth.getName(), req.date(), req.startTime(), req.endTime());
        return ResponseEntity.ok(Map.of("created", true));
    }

    record ClinicInfoRequest(String name, String address, String phone, String description) {}
    record ServiceRequest(String serviceName, String category, String description,
                          BigDecimal price, Integer durationMinutes, Boolean available) {}
    record SlotRequest(String date, String startTime, String endTime) {}

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable Long id, Authentication auth) {
        dashboardService.deleteSlot(auth.getName(), id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

}