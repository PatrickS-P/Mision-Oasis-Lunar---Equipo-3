package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UI-2 · Modelo de telemetría aislado con Mockito")
class ModeloTelemetriaTest {

    @Test
    @DisplayName("calcula altitud, distancia lunar y magnitud de velocidad")
    void calculaTelemetriaDesdeEstadoSimulado() {
        EstadoTelemetria estado = mock(EstadoTelemetria.class);

        when(estado.posicionNaveMetros())
                .thenReturn(new Vector3D(6_000.0, 8_000.0, 0.0));
        when(estado.posicionLunaMetros())
                .thenReturn(new Vector3D(16_000.0, 8_000.0, 0.0));
        when(estado.velocidadNaveMetrosSegundo())
                .thenReturn(new Vector3D(3_000.0, 4_000.0, 0.0));
        when(estado.altitudTerrestreMetros())
                .thenReturn(120_000.0);

        PuntoTelemetria punto =
                ModeloTelemetria.calcular(3600.0, estado);

        assertEquals(120.0, punto.altitudKm(), 1.0e-12);
        assertEquals(10.0, punto.distanciaLunarKm(), 1.0e-12);
        assertEquals(5.0, punto.velocidadKmS(), 1.0e-12);
        assertEquals(6.0, punto.xKm(), 1.0e-12);
        assertEquals(8.0, punto.yKm(), 1.0e-12);

        verify(estado).posicionNaveMetros();
        verify(estado).posicionLunaMetros();
        verify(estado).velocidadNaveMetrosSegundo();
        verify(estado).altitudTerrestreMetros();
    }

    @Test
    @DisplayName("genera textos de presentación sin iniciar JavaFX")
    void generaPresentacionParaLaVista() {
        PuntoTelemetria punto = new PuntoTelemetria(
                7200.0,
                1.0,
                2.0,
                3.0,
                185.5,
                10_000.25,
                7.6543
        );

        ModeloTelemetria.TelemetriaPresentacion vista =
                ModeloTelemetria.presentar(punto);

        assertEquals("Tiempo: 2.00 horas", vista.tiempo());
        assertEquals("Altitud terrestre: 185.50 km", vista.altitud());
        assertEquals(
                "Distancia lunar: 10000.25 km",
                vista.distanciaLunar()
        );
        assertEquals("Velocidad: 7.654 km/s", vista.velocidad());
    }
}
