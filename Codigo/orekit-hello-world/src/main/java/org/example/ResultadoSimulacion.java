package org.example;

import java.util.List;
import java.util.Optional;

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
     * Crea un resultado inmutable.
     *
     * @param puntos trayectoria precalculada
     * @param indicePeriapsisLunar índice del punto más cercano a la Luna
     * @param indiceReentrada índice del punto de reentrada o -1
     * @param fuenteDatos ubicación de los datos Orekit utilizados
     */
    public ResultadoSimulacion {
        puntos = List.copyOf(puntos);
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
