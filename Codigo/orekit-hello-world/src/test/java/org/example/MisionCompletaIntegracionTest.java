package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Integración · Simulación completa y consumo por la UI")
class MisionCompletaIntegracionTest {

    @Test
    @DisplayName("ejecuta órbita, TLI, sobrevuelo lunar y reentrada de extremo a extremo")
    void ejecutaMisionCompleta() {
        ResultadoSimulacion resultado = ResultadoMisionPrueba.obtener();

        assertTrue(resultado.puntos().size() >= 500);
        assertTrue(resultado.altitudPeriapsisLunarKm() > 0.0);
        assertTrue(resultado.reentrada().isPresent());

        PuntoTelemetria ultimo = resultado.puntos()
                .get(resultado.puntos().size() - 1);
        ModeloTelemetria.TelemetriaPresentacion presentacion =
                ModeloTelemetria.presentar(ultimo);

        assertFalse(presentacion.tiempo().isBlank());
        assertFalse(presentacion.altitud().isBlank());
        assertFalse(presentacion.distanciaLunar().isBlank());
        assertFalse(presentacion.velocidad().isBlank());
        assertTrue(presentacion.altitud().contains("120"));
    }

    @Test
    @DisplayName("UI-4: parámetros distintos producen trayectorias distintas")
    void parametrosDistintosCambianTrayectoria() {
        ParametrosSimulacion base = new ParametrosSimulacion(
                185.0,
                3220.0,
                4320.0,
                72.0,
                1200.0,
                1.0,
                0.0,
                -0.20
        );
        ParametrosSimulacion alternativa = new ParametrosSimulacion(
                185.0,
                3260.0,
                4320.0,
                72.0,
                1200.0,
                1.0,
                0.05,
                -0.10
        );

        ResultadoSimulacion resultadoBase = MotorOrbital.simular(base);
        ResultadoSimulacion resultadoAlternativo =
                MotorOrbital.simular(alternativa);

        PuntoTelemetria finalBase = resultadoBase.puntos()
                .get(resultadoBase.puntos().size() - 1);
        PuntoTelemetria finalAlternativo = resultadoAlternativo.puntos()
                .get(resultadoAlternativo.puntos().size() - 1);

        double diferencia = Math.hypot(
                finalBase.xKm() - finalAlternativo.xKm(),
                finalBase.yKm() - finalAlternativo.yKm()
        );

        assertTrue(diferencia > 1.0);
        assertNotEquals(
                resultadoBase.periapsisLunar().distanciaLunarKm(),
                resultadoAlternativo.periapsisLunar().distanciaLunarKm(),
                0.001
        );
        assertEquals(
                resultadoBase.puntos().get(0).tiempoSegundos(),
                resultadoAlternativo.puntos().get(0).tiempoSegundos(),
                1.0e-6
        );
    }
}
