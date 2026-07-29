package org.example;

/**
 * Comparte la simulación predeterminada entre pruebas para evitar
 * ejecutar varias veces la propagación completa dentro del mismo JVM.
 */
final class ResultadoMisionPrueba {

    private static ResultadoSimulacion resultado;

    private ResultadoMisionPrueba() {
    }

    static synchronized ResultadoSimulacion obtener() {
        if (resultado == null) {
            resultado = MotorOrbital.simular(
                    ParametrosSimulacion.valoresPredeterminados()
            );
        }
        return resultado;
    }
}
