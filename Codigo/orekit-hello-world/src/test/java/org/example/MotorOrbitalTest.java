package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OAM-5 a OAM-7 · Propagación y eventos")
class MotorOrbitalTest {

    @Test
    @DisplayName("OAM-5: genera al menos 500 puntos con tiempo creciente")
    void generaTrayectoriaContinua() {
        ResultadoSimulacion resultado = ResultadoMisionPrueba.obtener();

        assertTrue(resultado.puntos().size() >= 500);

        for (int i = 1; i < resultado.puntos().size(); i++) {
            assertTrue(
                    resultado.puntos().get(i).tiempoSegundos()
                    > resultado.puntos().get(i - 1).tiempoSegundos(),
                    "Las marcas de tiempo deben ser estrictamente crecientes."
            );
        }
    }

    @Test
    @DisplayName("OAM-6: detecta periapsis lunar en un rango físico")
    void detectaPeriapsisLunar() {
        ResultadoSimulacion resultado = ResultadoMisionPrueba.obtener();
        PuntoTelemetria periapsis = resultado.periapsisLunar();

        assertTrue(resultado.altitudPeriapsisLunarKm() >= 100.0);
        assertTrue(resultado.altitudPeriapsisLunarKm() <= 15_000.0);
        assertTrue(periapsis.tiempoSegundos() > 12.0 * 3600.0);
        assertTrue(periapsis.tiempoSegundos() < 120.0 * 3600.0);
    }

    @Test
    @DisplayName("OAM-7: detecta reentrada a aproximadamente 120 km")
    void detectaInterfazReentrada() {
        ResultadoSimulacion resultado = ResultadoMisionPrueba.obtener();

        assertTrue(resultado.reentrada().isPresent());
        PuntoTelemetria reentrada = resultado.reentrada().orElseThrow();

        assertEquals(120.0, reentrada.altitudKm(), 1.0);
        assertTrue(
                reentrada.tiempoSegundos()
                > resultado.periapsisLunar().tiempoSegundos()
        );

        PuntoTelemetria ultimo = resultado.puntos()
                .get(resultado.puntos().size() - 1);
        assertEquals(
                reentrada.tiempoSegundos(),
                ultimo.tiempoSegundos(),
                1.0e-6,
                "La trayectoria entregada a la UI debe terminar en reentrada."
        );
        assertFalse(resultado.fuenteDatos().isBlank());
    }
}
