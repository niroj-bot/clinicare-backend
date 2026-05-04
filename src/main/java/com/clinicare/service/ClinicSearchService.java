package com.clinicare.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.clinicare.model.Clinic;
import com.clinicare.model.ClinicService;
import com.clinicare.model.TimeSlot;
import com.clinicare.repository.ClinicRepository;
import com.clinicare.repository.ClinicServiceRepository;
import com.clinicare.repository.TimeSlotRepository;
import com.clinicare.util.DistanceCalculator;
import com.clinicare.util.SmartRankingEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicSearchService {

    private final ClinicRepository clinicRepo;
    private final ClinicServiceRepository serviceRepo;
    private final TimeSlotRepository slotRepo;
    private final DistanceCalculator distanceCalc;
    private final SmartRankingEngine rankingEngine;

    public List<Map<String, Object>> search(
            String keyword,
            Double userLat,
            Double userLng,
            String sortBy,
            BigDecimal maxPrice,
            Double maxDistanceKm,
            Double minRating
    ) {
        // 1. Fetch matching clinics
        List<Clinic> clinics = (keyword != null && !keyword.isBlank())
                ? clinicRepo.searchByServiceOrName(keyword)
                : clinicRepo.findByActiveTrue();

        List<Long> clinicIds = clinics.stream().map(Clinic::getId).toList();

        // 2. Fetch ALL services and slots in just 2 queries
        Map<Long, List<ClinicService>> servicesMap = serviceRepo
                .findByClinicIdInAndAvailableTrue(clinicIds)
                .stream().collect(Collectors.groupingBy(s -> s.getClinic().getId()));

        Map<Long, List<TimeSlot>> slotsMap = slotRepo
                .findByClinicIdInAndIsBookedFalseOrderByDateAscStartTimeAsc(clinicIds)
                .stream().collect(Collectors.groupingBy(s -> s.getClinic().getId()));

        // 3. Build ranking inputs
        List<SmartRankingEngine.RankingInput> inputs = new ArrayList<>();
        Map<Long, Clinic> clinicMap = new LinkedHashMap<>();

        for (Clinic clinic : clinics) {
            if (!clinic.getActive()) continue;

            List<ClinicService> services = servicesMap.getOrDefault(clinic.getId(), List.of());
            BigDecimal lowestPrice = services.stream()
                    .map(ClinicService::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            if (maxPrice != null && lowestPrice.compareTo(maxPrice) > 0) continue;

            double distance = 0.0;
            int travelMins = 0;
            if (userLat != null && userLng != null
                    && clinic.getLatitude() != null && clinic.getLongitude() != null) {
                distance = distanceCalc.calculate(userLat, userLng, clinic.getLatitude(), clinic.getLongitude());
                travelMins = distanceCalc.estimateTravelMinutes(distance);
            }

            if (maxDistanceKm != null && distance > maxDistanceKm) continue;

            double rating = clinic.getAverageRating() != null ? clinic.getAverageRating() : 0.0;
            if (minRating != null && rating < minRating) continue;

            List<TimeSlot> slots = slotsMap.getOrDefault(clinic.getId(), List.of());
            int waitMins = slots.isEmpty() ? 999 : slots.get(0).getWaitMinutes();

            SmartRankingEngine.RankingInput input = new SmartRankingEngine.RankingInput();
            input.clinicId      = clinic.getId();
            input.price         = lowestPrice.doubleValue();
            input.distanceKm    = distance;
            input.rating        = rating;
            input.travelMinutes = travelMins;
            input.waitMinutes   = waitMins;

            inputs.add(input);
            clinicMap.put(clinic.getId(), clinic);
        }

        // 4. Compute scores
        rankingEngine.computeScores(inputs);

        // 5. Sort
        Comparator<SmartRankingEngine.RankingInput> comparator = switch (sortBy != null ? sortBy : "smart") {
            case "price"    -> Comparator.comparingDouble(i -> i.price);
            case "distance" -> Comparator.comparingDouble(i -> i.distanceKm);
            case "rating"   -> Comparator.comparingDouble((SmartRankingEngine.RankingInput i) -> i.rating).reversed();
            default         -> Comparator.comparingDouble((SmartRankingEngine.RankingInput i) -> i.smartScore).reversed();
        };
        inputs.sort(comparator);

        // 6. Build response
        return inputs.stream().map(input -> {
            Clinic clinic = clinicMap.get(input.clinicId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id",              clinic.getId());
            result.put("name",            clinic.getName());
            result.put("address",         clinic.getAddress());
            result.put("imageUrl",        clinic.getImageUrl());
            result.put("averageRating",   Math.round(input.rating * 10.0) / 10.0);
            result.put("reviewCount",     clinic.getReviewCount());
            result.put("latitude",        clinic.getLatitude());
            result.put("longitude",       clinic.getLongitude());
            result.put("distanceKm",      Math.round(input.distanceKm * 10.0) / 10.0);
            result.put("travelMinutes",   input.travelMinutes);
            result.put("lowestPrice",     input.price);
            result.put("priceScore",      input.priceScore);
            result.put("smartScore",      input.smartScore);
            result.put("tags",            input.tags);
            result.put("timeToTreatment", input.travelMinutes + input.waitMinutes);
            return result;
        }).collect(Collectors.toList());
    }
}