package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Modelo ResultadoSimulacion")
class ResultadoSimulacionTest {

    @Test
    @DisplayName("protege la trayectoria frente a modificaciones externas")
    void resultadoEsInmutable() {
        List<PuntoTelemetria> originales = new ArrayList<>();
        originales.add(new PuntoTelemetria(
                0, 1, 2, 3, 185, 10_000, 7.8
        ));

        ResultadoSimulacion resultado = new ResultadoSimulacion(
                originales,
                0,
                -1,
                "orekit-data"
        );
        originales.clear();

        assertEquals(1, resultado.puntos().size());
        assertTrue(resultado.reentrada().isEmpty());
        assertThrows(
                UnsupportedOperationException.class,
                () -> resultado.puntos().clear()
        );
    }
}
