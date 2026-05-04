package com.clinicare.util;

import org.springframework.stereotype.Component;

/**
 * Haversine formula — calculates straight-line distance between two
 * lat/lng points on Earth's surface. Used when Google Maps API is not available.
 *
 * Result is in kilometres.
 */
@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double calculate(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Rough travel time estimate: assume average 30 km/h in city traffic.
     * Returns minutes.
     */
    public int estimateTravelMinutes(double distanceKm) {
        double avgSpeedKmH = 30.0;
        return (int) Math.ceil((distanceKm / avgSpeedKmH) * 60);
    }
}
