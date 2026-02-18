package com.news.retrieval.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final double KM_PER_DEGREE_LAT = 111.0;


    private GeoUtils() {}

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }


    public static String toGeoCell(double lat, double lon) {
        BigDecimal roundedLat = BigDecimal.valueOf(lat).setScale(1, RoundingMode.HALF_UP);
        BigDecimal roundedLon = BigDecimal.valueOf(lon).setScale(1, RoundingMode.HALF_UP);
        return roundedLat + "_" + roundedLon;
    }


    public static double latDeltaForRadius(double radiusKm) {
        return radiusKm / KM_PER_DEGREE_LAT;
    }

    public static double lonDeltaForRadius(double lat, double radiusKm) {
        return radiusKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
    }

    public static Set<String> getGeoCellsWithinRadius(double lat, double lon, double radiusKm) {
        Set<String> cells = new HashSet<>();

        double latDelta = latDeltaForRadius(radiusKm);
        double lonDelta = lonDeltaForRadius(lat, radiusKm);

        double cellSize = 0.1;
        for (double currLat = lat - latDelta; currLat <= lat + latDelta; currLat += cellSize) {
            for (double currLon = lon - lonDelta; currLon <= lon + lonDelta; currLon += cellSize) {
                if (haversineDistance(lat, lon, currLat, currLon) <= radiusKm) {
                    cells.add(toGeoCell(currLat, currLon));
                }
            }
        }

        cells.add(toGeoCell(lat, lon));
        return cells;
    }
}
