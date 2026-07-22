package org.example;

/**
 * Punto precalculado de trayectoria y telemetría.
 *
 * @param tiempoSegundos tiempo transcurrido desde el inicio
 * @param xKm posición X en kilómetros
 * @param yKm posición Y en kilómetros
 * @param zKm posición Z en kilómetros
 * @param altitudKm altitud aproximada sobre la Tierra
 * @param distanciaLunarKm distancia entre la nave y la Luna
 * @param velocidadKmS velocidad de la nave
 */
public record PuntoTelemetria(
        double tiempoSegundos,
        double xKm,
        double yKm,
        double zKm,
        double altitudKm,
        double distanciaLunarKm,
        double velocidadKmS
) {
}
