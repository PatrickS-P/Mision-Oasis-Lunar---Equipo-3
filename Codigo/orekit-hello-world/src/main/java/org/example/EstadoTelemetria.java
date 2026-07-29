package org.example;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

/**
 * Contrato mínimo que el modelo de telemetría necesita de un estado físico.
 * Facilita probar la lógica de interfaz con objetos simulados mediante Mockito.
 */
public interface EstadoTelemetria {

    Vector3D posicionNaveMetros();

    Vector3D posicionLunaMetros();

    Vector3D velocidadNaveMetrosSegundo();

    double altitudTerrestreMetros();
}
