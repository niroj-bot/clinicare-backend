package com.clinicare.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Smart Ranking Algorithm
 *
 * Score = (Price * 0.40) + (Distance * 0.35) + (Rating * 0.25)
 *
 * Each factor is normalized 0.0–1.0 within the result set, then
 * inverted where needed (lower price = higher score, closer = higher score).
 *
 * Final score is 0.0–1.0 (higher = better).
 */
@Component
public class SmartRankingEngine {

    // Weights — must sum to 1.0
    private static final double PRICE_WEIGHT    = 0.40;
    private static final double DISTANCE_WEIGHT = 0.35;
    private static final double RATING_WEIGHT   = 0.25;

    // Price score thresholds (vs average price in result set)
    private static final double BEST_DEAL_THRESHOLD  = 0.80;  // <= 80% of avg → Best Deal
    private static final double EXPENSIVE_THRESHOLD  = 1.25;  // >= 125% of avg → Expensive

    /**
     * Compute smart scores for a list of results.
     * Mutates each RankingInput by setting its smartScore.
     */
    public void computeScores(List<RankingInput> inputs) {
        if (inputs == null || inputs.isEmpty()) return;

        // Find min/max for normalization
        double minPrice    = inputs.stream().mapToDouble(i -> i.price).min().orElse(0);
        double maxPrice    = inputs.stream().mapToDouble(i -> i.price).max().orElse(1);
        double minDist     = inputs.stream().mapToDouble(i -> i.distanceKm).min().orElse(0);
        double maxDist     = inputs.stream().mapToDouble(i -> i.distanceKm).max().orElse(1);
        double avgPrice    = inputs.stream().mapToDouble(i -> i.price).average().orElse(1);

        for (RankingInput input : inputs) {
            // Normalize then invert (lower price = score closer to 1)
            double priceScore    = normalize(input.price, minPrice, maxPrice, true);
            double distanceScore = normalize(input.distanceKm, minDist, maxDist, true);
            double ratingScore   = input.rating / 5.0;  // already 0–5, normalize to 0–1

            double smartScore = (priceScore * PRICE_WEIGHT)
                    + (distanceScore * DISTANCE_WEIGHT)
                    + (ratingScore * RATING_WEIGHT);

            input.smartScore = Math.round(smartScore * 1000.0) / 1000.0;

            // Fair Price label
            double ratio = avgPrice > 0 ? input.price / avgPrice : 1.0;
            if (ratio <= BEST_DEAL_THRESHOLD) {
                input.priceScore = "BEST_DEAL";
            } else if (ratio >= EXPENSIVE_THRESHOLD) {
                input.priceScore = "EXPENSIVE";
            } else {
                input.priceScore = "FAIR_PRICE";
            }
        }

        // Assign smart tags based on relative ranks
        RankingInput bestValue = inputs.stream()
                .max((a, b) -> Double.compare(a.smartScore, b.smartScore)).orElse(null);
        RankingInput closest   = inputs.stream()
                .min((a, b) -> Double.compare(a.distanceKm, b.distanceKm)).orElse(null);
        RankingInput fastest   = inputs.stream()
                .min((a, b) -> Integer.compare(a.travelMinutes + a.waitMinutes,
                        b.travelMinutes + b.waitMinutes)).orElse(null);

        if (bestValue != null) bestValue.tags.add("Best Value");
        if (closest   != null) closest.tags.add("Closest");
        if (fastest   != null) fastest.tags.add("Fastest");
    }

    /**
     * Normalize a value to 0–1. If invert=true, lower raw value = higher score.
     */
    private double normalize(double value, double min, double max, boolean invert) {
        if (max == min) return 1.0;
        double normalized = (value - min) / (max - min);
        return invert ? 1.0 - normalized : normalized;
    }

    /**
     * Simple mutable struct for ranking computation.
     */
    public static class RankingInput {
        public Long clinicId;
        public double price;
        public double distanceKm;
        public double rating;
        public int travelMinutes;
        public int waitMinutes;

        // Output fields (written by computeScores)
        public double smartScore;
        public String priceScore;
        public List<String> tags = new java.util.ArrayList<>();
    }
}
