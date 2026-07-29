package org.example;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/**
 * Lógica de modelo consumida por la interfaz de usuario.
 *
 * Calcula la telemetría a partir de un estado físico y genera textos de
 * presentación sin depender de componentes JavaFX.
 */
public final class ModeloTelemetria {

    private ModeloTelemetria() {
        // Clase de utilidad.
    }

    /**
     * Calcula un punto de telemetría a partir de un estado físico abstracto.
     *
     * @param tiempoSegundos tiempo desde el inicio
     * @param estado estado físico de la nave
     * @return telemetría calculada
     */
    public static PuntoTelemetria calcular(
            double tiempoSegundos,
            EstadoTelemetria estado
    ) {
        Vector3D posicion = estado.posicionNaveMetros();
        Vector3D posicionLuna = estado.posicionLunaMetros();
        Vector3D velocidad = estado.velocidadNaveMetrosSegundo();

        return new PuntoTelemetria(
                tiempoSegundos,
                posicion.getX() / 1000.0,
                posicion.getY() / 1000.0,
                posicion.getZ() / 1000.0,
                estado.altitudTerrestreMetros() / 1000.0,
                Vector3D.distance(posicion, posicionLuna) / 1000.0,
                velocidad.getNorm() / 1000.0
        );
    }

    /**
     * Genera los textos de telemetría mostrados por JavaFX.
     *
     * @param punto punto físico ya calculado
     * @return textos de presentación
     */
    public static TelemetriaPresentacion presentar(
            PuntoTelemetria punto
    ) {
        return new TelemetriaPresentacion(
                String.format(
                        "Tiempo: %.2f horas",
                        punto.tiempoSegundos() / 3600.0
                ),
                String.format(
                        "Altitud terrestre: %.2f km",
                        punto.altitudKm()
                ),
                String.format(
                        "Distancia lunar: %.2f km",
                        punto.distanciaLunarKm()
                ),
                String.format(
                        "Velocidad: %.3f km/s",
                        punto.velocidadKmS()
                )
        );
    }

    /**
     * Textos listos para ser consumidos por la vista.
     */
    public record TelemetriaPresentacion(
            String tiempo,
            String altitud,
            String distanciaLunar,
            String velocidad
    ) {
    }
}
