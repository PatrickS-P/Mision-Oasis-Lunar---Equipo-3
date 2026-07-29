package org.example;

import java.util.List;
import java.util.Optional;

import org.orekit.utils.Constants;

/**
 * Resultado completo de una propagación orbital.
 *
 * @param puntos trayectoria precalculada
 * @param indicePeriapsisLunar posición del punto más cercano a la Luna
 * @param indiceReentrada posición donde se detectó la interfaz de reentrada
 * @param fuenteDatos ubicación de los datos Orekit utilizados
 */
public record ResultadoSimulacion(
        List<PuntoTelemetria> puntos,
        int indicePeriapsisLunar,
        int indiceReentrada,
        String fuenteDatos
) {

    /**
     * Crea un resultado inmutable y valida los índices asociados.
     */
    public ResultadoSimulacion {
        puntos = List.copyOf(puntos);

        if (puntos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El resultado debe contener puntos de trayectoria."
            );
        }

        if (
                indicePeriapsisLunar < 0
                || indicePeriapsisLunar >= puntos.size()
        ) {
            throw new IllegalArgumentException(
                    "El índice de periapsis lunar no es válido."
            );
        }

        if (indiceReentrada >= puntos.size()) {
            throw new IllegalArgumentException(
                    "El índice de reentrada no es válido."
            );
        }
    }

    /**
     * Obtiene el punto de mínima distancia lunar.
     *
     * @return telemetría del periapsis lunar
     */
    public PuntoTelemetria periapsisLunar() {
        return puntos.get(indicePeriapsisLunar);
    }

    /**
     * Calcula la altitud del periapsis sobre la superficie lunar.
     *
     * @return altitud sobre la Luna en kilómetros
     */
    public double altitudPeriapsisLunarKm() {
        return periapsisLunar().distanciaLunarKm()
                - Constants.MOON_EQUATORIAL_RADIUS / 1000.0;
    }

    /**
     * Obtiene el punto de reentrada cuando existe.
     *
     * @return punto de reentrada o vacío
     */
    public Optional<PuntoTelemetria> reentrada() {
        if (indiceReentrada < 0) {
            return Optional.empty();
        }
        return Optional.of(puntos.get(indiceReentrada));
    }
}
