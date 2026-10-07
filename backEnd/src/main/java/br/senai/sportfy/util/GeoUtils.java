package br.senai.sportfy.util;

/**
 * Cálculos de geolocalização usados na busca por proximidade.
 */
public final class GeoUtils {

    public static final double RAIO_TERRA_KM = 6371.0;

    private GeoUtils() {
    }

    /** Distância em km entre dois pontos (fórmula de Haversine). */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_KM * c;
    }

    /** Arredonda para 1 casa decimal (ex.: 3.27 -> 3.3). */
    public static double arredondar(double km) {
        return Math.round(km * 10.0) / 10.0;
    }

    /**
     * Retângulo (latMin, latMax, lonMin, lonMax) que contém o círculo de raio informado.
     * Usado para filtrar no banco de forma rápida antes do cálculo exato.
     */
    public static double[] caixaDelimitadora(double lat, double lon, double raioKm) {
        double deltaLat = Math.toDegrees(raioKm / RAIO_TERRA_KM);
        double cosLat = Math.cos(Math.toRadians(lat));
        double deltaLon = cosLat < 1e-6 ? 180.0 : Math.toDegrees(raioKm / (RAIO_TERRA_KM * cosLat));
        return new double[] {
                Math.max(-90.0, lat - deltaLat),
                Math.min(90.0, lat + deltaLat),
                Math.max(-180.0, lon - deltaLon),
                Math.min(180.0, lon + deltaLon)
        };
    }
}
