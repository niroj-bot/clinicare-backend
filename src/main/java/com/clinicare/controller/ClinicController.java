package com.clinicare.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.clinicare.model.Clinic;
import com.clinicare.model.ClinicService;
import com.clinicare.model.TimeSlot;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ClinicServiceRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.service.ClinicSearchService;
import com.clinicare.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicSearchService searchService;
    private final ClinicRepository clinicRepo;
    private final ClinicServiceRepository serviceRepo;
    private final TimeSlotRepository slotRepo;
    private final ReviewService reviewService;

    /**
     * GET /api/clinics/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng,
            @RequestParam(defaultValue = "smart") String sortBy,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) Double minRating
    ) {
        List<Map<String, Object>> results = searchService.search(
                keyword, userLat, userLng, sortBy, maxPrice, maxDistanceKm, minRating
        );
        return ResponseEntity.ok(results);
    }

   
    @GetMapping("/{id}")
    public ResponseEntity<?> getClinic(
            @PathVariable Long id,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng
    ) {
        Clinic clinic = clinicRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Clinic not found"));

        // Single query for both today and tomorrow
        List<ClinicService> services = serviceRepo.findByClinicIdAndAvailableTrue(id);
        List<TimeSlot> allSlots = slotRepo.findByClinicIdAndDateBetweenAndIsBookedFalseOrderByDateAscStartTimeAsc(
                id, LocalDate.now(), LocalDate.now().plusDays(1));
        var reviews = reviewService.getClinicReviews(id);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id",            clinic.getId());
        response.put("name",          clinic.getName());
        response.put("address",       clinic.getAddress());
        response.put("phone",         clinic.getPhone());
        response.put("description",   clinic.getDescription());
        response.put("imageUrl",      clinic.getImageUrl());
        response.put("averageRating", clinic.getAverageRating());
        response.put("reviewCount",   clinic.getReviewCount());
        response.put("latitude",      clinic.getLatitude());
        response.put("longitude",     clinic.getLongitude());

        response.put("services", services.stream().map(s -> Map.of(
                "id",              s.getId(),
                "serviceName",     s.getServiceName(),
                "category",        s.getCategory(),
                "price",           s.getPrice(),
                "durationMinutes", s.getDurationMinutes()
        )).toList());

        response.put("availableSlots", allSlots.stream().map(sl -> Map.of(
                "id",          sl.getId(),
                "date",        sl.getDate().toString(),
                "startTime",   sl.getStartTime().toString(),
                "endTime",     sl.getEndTime().toString(),
                "waitMinutes", sl.getWaitMinutes()
        )).toList());

        response.put("recentReviews", reviews.stream().limit(5).map(r -> Map.of(
                "id",           r.getId(),
                "reviewerName", r.getReviewerName() != null ? r.getReviewerName() : "Anonymous",
                "rating",       r.getRating(),
                "comment",      r.getComment() != null ? r.getComment() : "",
                "createdAt",    r.getCreatedAt().toString()
        )).toList());

        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/compare")
    public ResponseEntity<?> compare(
            @RequestParam List<Long> ids,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng
    ) {
        // 2 queries total instead of 2 per clinic
        List<Clinic> clinics = clinicRepo.findAllById(ids);
        Map<Long, List<ClinicService>> servicesMap = serviceRepo
                .findByClinicIdInAndAvailableTrue(ids)
                .stream().collect(Collectors.groupingBy(s -> s.getClinic().getId()));

        List<Map<String, Object>> results = clinics.stream().map(c -> {
            List<ClinicService> services = servicesMap.getOrDefault(c.getId(), List.of());
            BigDecimal lowestPrice = services.stream()
                    .map(ClinicService::getPrice).filter(Objects::nonNull)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id",            c.getId());
            row.put("name",          c.getName());
            row.put("address",       c.getAddress());
            row.put("averageRating", c.getAverageRating());
            row.put("reviewCount",   c.getReviewCount());
            row.put("lowestPrice",   lowestPrice);
            row.put("latitude",      c.getLatitude()  != null ? c.getLatitude()  : 0);
            row.put("longitude",     c.getLongitude() != null ? c.getLongitude() : 0);
            return row;
        }).toList();

        return ResponseEntity.ok(results);
    }

    
    @GetMapping("/urgency")
    public ResponseEntity<?> urgencyMode(
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng
    ) {
        List<Map<String, Object>> results = searchService.search(
                null, userLat, userLng, "smart", null, null, null
        );
        results.sort(Comparator.comparingInt(m -> (Integer) m.get("timeToTreatment")));
        return ResponseEntity.ok(results.stream().limit(3).toList());
    }

   
    @GetMapping("/{id}/slots")
    public ResponseEntity<?> getSlotsByDate(
            @PathVariable Long id,
            @RequestParam(required = false) String date) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<TimeSlot> slots = slotRepo
                .findByClinicIdAndDateAndIsBookedFalseOrderByStartTime(id, d);

        // Filter out past time slots if querying today
        if (d.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            slots = slots.stream()
                    .filter(s -> s.getStartTime().isAfter(now))
                    .collect(java.util.stream.Collectors.toList());
        }

        return ResponseEntity.ok(slots.stream().map(s -> Map.of(
                "id",        s.getId(),
                "date",      s.getDate().toString(),
                "startTime", s.getStartTime().toString(),
                "endTime",   s.getEndTime().toString()
        )).collect(java.util.stream.Collectors.toList()));
    }

}