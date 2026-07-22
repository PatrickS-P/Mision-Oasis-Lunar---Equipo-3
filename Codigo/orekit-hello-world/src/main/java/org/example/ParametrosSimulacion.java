package org.example;

/**
 * Parámetros configurables de una simulación de la Misión Oasis Lunar.
 *
 * @param altitudInicialKm altitud inicial sobre la Tierra en kilómetros
 * @param deltaVMps magnitud de la maniobra TLI en metros por segundo
 * @param retrasoTliSegundos tiempo desde el inicio hasta la maniobra TLI
 * @param duracionHoras duración total de la propagación
 * @param pasoMuestreoSegundos intervalo entre puntos de telemetría
 * @param direccionTliX componente X/T de la dirección TLI
 * @param direccionTliY componente Y/N de la dirección TLI
 * @param direccionTliZ componente Z/W de la dirección TLI
 */
public record ParametrosSimulacion(
        double altitudInicialKm,
        double deltaVMps,
        double retrasoTliSegundos,
        double duracionHoras,
        double pasoMuestreoSegundos,
        double direccionTliX,
        double direccionTliY,
        double direccionTliZ
) {

    /**
     * Verifica que los parámetros sean utilizables.
     */
    public ParametrosSimulacion {

        if (
                !Double.isFinite(altitudInicialKm)
                || altitudInicialKm < 120.0
                || altitudInicialKm > 2000.0
        ) {
            throw new IllegalArgumentException(
                    "La altitud inicial debe estar entre 120 y 2000 km."
            );
        }

        if (
                !Double.isFinite(deltaVMps)
                || deltaVMps < 0.0
                || deltaVMps > 5000.0
        ) {
            throw new IllegalArgumentException(
                    "El delta-v debe estar entre 0 y 5 km/s."
            );
        }

        if (
                !Double.isFinite(retrasoTliSegundos)
                || retrasoTliSegundos < 0.0
        ) {
            throw new IllegalArgumentException(
                    "La época TLI no puede ser negativa."
            );
        }

        if (
                !Double.isFinite(duracionHoras)
                || duracionHoras <= 0.0
                || duracionHoras > 240.0
        ) {
            throw new IllegalArgumentException(
                    "La duración debe estar entre 0 y 240 horas."
            );
        }

        if (
                !Double.isFinite(pasoMuestreoSegundos)
                || pasoMuestreoSegundos <= 0.0
        ) {
            throw new IllegalArgumentException(
                    "El paso de muestreo debe ser mayor que cero."
            );
        }

        if (
                !Double.isFinite(direccionTliX)
                || !Double.isFinite(direccionTliY)
                || !Double.isFinite(direccionTliZ)
        ) {
            throw new IllegalArgumentException(
                    "La dirección TLI debe contener valores numéricos."
            );
        }

        double normaDireccion = Math.sqrt(
                direccionTliX * direccionTliX
                + direccionTliY * direccionTliY
                + direccionTliZ * direccionTliZ
        );

        if (normaDireccion < 1.0e-12) {
            throw new IllegalArgumentException(
                    "La dirección TLI no puede ser el vector 0, 0, 0."
            );
        }
    }

    /**
     * Constructor compatible con configuraciones anteriores.
     *
     * Utiliza una dirección tangencial positiva predeterminada.
     *
     * @param altitudInicialKm altitud inicial en kilómetros
     * @param deltaVMps delta-v en metros por segundo
     * @param retrasoTliSegundos retraso de la maniobra
     * @param duracionHoras duración de la propagación
     * @param pasoMuestreoSegundos paso de telemetría
     */
    public ParametrosSimulacion(
            double altitudInicialKm,
            double deltaVMps,
            double retrasoTliSegundos,
            double duracionHoras,
            double pasoMuestreoSegundos
    ) {
        this(
                altitudInicialKm,
                deltaVMps,
                retrasoTliSegundos,
                duracionHoras,
                pasoMuestreoSegundos,
                1.0,
                0.0,
                0.0
        );
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
                600.0,
                1.0,
                0.0,
                0.0
        );
    }
}
