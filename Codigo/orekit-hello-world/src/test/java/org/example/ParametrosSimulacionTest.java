package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("UI-3 · Validación de parámetros")
class ParametrosSimulacionTest {

    @Test
    @DisplayName("conserva los valores predeterminados del MVS")
    void conservaValoresPredeterminados() {
        ParametrosSimulacion p =
                ParametrosSimulacion.valoresPredeterminados();

        assertEquals(185.0, p.altitudInicialKm());
        assertEquals(3220.0, p.deltaVMps());
        assertEquals(4320.0, p.retrasoTliSegundos());
        assertEquals(240.0, p.duracionHoras());
        assertEquals(600.0, p.pasoMuestreoSegundos());
    }

    @ParameterizedTest(name = "configuración válida #{index}")
    @MethodSource("parametrosValidos")
    @DisplayName("acepta límites válidos")
    void aceptaLimitesValidos(ParametrosSimulacion parametros) {
        assertDoesNotThrow(() -> new ParametrosSimulacion(
                parametros.altitudInicialKm(),
                parametros.deltaVMps(),
                parametros.retrasoTliSegundos(),
                parametros.duracionHoras(),
                parametros.pasoMuestreoSegundos(),
                parametros.direccionTliX(),
                parametros.direccionTliY(),
                parametros.direccionTliZ()
        ));
    }

    @ParameterizedTest(name = "configuración inválida #{index}")
    @MethodSource("parametrosInvalidos")
    @DisplayName("rechaza valores inválidos y no numéricos")
    void rechazaValoresInvalidos(
            double altitud,
            double deltaV,
            double retraso,
            double duracion,
            double paso,
            double x,
            double y,
            double z
    ) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ParametrosSimulacion(
                        altitud,
                        deltaV,
                        retraso,
                        duracion,
                        paso,
                        x,
                        y,
                        z
                )
        );
    }

    private static Stream<ParametrosSimulacion> parametrosValidos() {
        return Stream.of(
                new ParametrosSimulacion(
                        120.0, 0.0, 0.0, 0.1, 1.0,
                        1.0, 0.0, 0.0
                ),
                new ParametrosSimulacion(
                        2000.0, 5000.0, 10.0, 240.0, 3600.0,
                        0.0, 1.0, 0.0
                )
        );
    }

    private static Stream<Arguments> parametrosInvalidos() {
        return Stream.of(
                Arguments.of(119.9, 3220, 0, 10, 10, 1, 0, 0),
                Arguments.of(2000.1, 3220, 0, 10, 10, 1, 0, 0),
                Arguments.of(185, -1, 0, 10, 10, 1, 0, 0),
                Arguments.of(185, 5000.1, 0, 10, 10, 1, 0, 0),
                Arguments.of(185, 3220, -1, 10, 10, 1, 0, 0),
                Arguments.of(185, 3220, 0, 0, 10, 1, 0, 0),
                Arguments.of(185, 3220, 0, 241, 10, 1, 0, 0),
                Arguments.of(185, 3220, 0, 10, 0, 1, 0, 0),
                Arguments.of(185, 3220, 0, 10, 10, 0, 0, 0),
                Arguments.of(Double.NaN, 3220, 0, 10, 10, 1, 0, 0)
        );
    }
}
