package org.example;

/**
 * Parámetros configurables de una simulación de la Misión Oasis Lunar.
 *
 * @param altitudInicialKm altitud inicial sobre la Tierra en kilómetros
 * @param deltaVMps delta-v de la maniobra TLI en metros por segundo
 * @param retrasoTliSegundos tiempo desde el inicio hasta la maniobra TLI
 * @param duracionHoras duración total de la propagación
 * @param pasoMuestreoSegundos intervalo entre puntos de telemetría
 */
public record ParametrosSimulacion(
        double altitudInicialKm,
        double deltaVMps,
        double retrasoTliSegundos,
        double duracionHoras,
        double pasoMuestreoSegundos
) {

    /**
     * Verifica que todos los parámetros sean físicamente utilizables.
     *
     * @param altitudInicialKm altitud inicial sobre la Tierra en kilómetros
     * @param deltaVMps delta-v de la maniobra TLI en metros por segundo
     * @param retrasoTliSegundos tiempo desde el inicio hasta la maniobra
     * @param duracionHoras duración total de la propagación
     * @param pasoMuestreoSegundos intervalo entre puntos de telemetría
     */
    public ParametrosSimulacion {
        if (altitudInicialKm < 120.0 || altitudInicialKm > 2000.0) {
            throw new IllegalArgumentException(
                    "La altitud inicial debe estar entre 120 y 2000 km."
            );
        }
        if (deltaVMps < 0.0 || deltaVMps > 5000.0) {
            throw new IllegalArgumentException(
                    "El delta-v debe estar entre 0 y 5000 m/s."
            );
        }
        if (retrasoTliSegundos < 0.0) {
            throw new IllegalArgumentException(
                    "La época TLI no puede ser negativa."
            );
        }
        if (duracionHoras <= 0.0 || duracionHoras > 240.0) {
            throw new IllegalArgumentException(
                    "La duración debe estar entre 0 y 240 horas."
            );
        }
        if (pasoMuestreoSegundos <= 0.0) {
            throw new IllegalArgumentException(
                    "El paso de muestreo debe ser mayor que cero."
            );
        }
    }

    /**
     * Parámetros iniciales recomendados para el prototipo.
     *
     * @return configuración predeterminada
     */
    public static ParametrosSimulacion valoresPredeterminados() {
        return new ParametrosSimulacion(
                185.0,
                3150.0,
                600.0,
                120.0,
                600.0
        );
    }
}
