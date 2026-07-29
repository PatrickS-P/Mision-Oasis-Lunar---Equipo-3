package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.orekit.utils.Constants;

/**
 * Busca una trayectoria con sobrevuelo lunar seguro
 * y posterior regreso hacia la Tierra.
 */
public final class BuscadorTrayectoriaLunar {

    private static final double HORA_SEGUNDOS = 3600.0;

    /**
     * Altitud mínima aceptada sobre la Luna.
     */
    private static final double ALTITUD_LUNAR_MINIMA_KM = 100.0;

    /**
     * Altitud máxima para considerar que hubo
     * un sobrevuelo lunar cercano.
     */
    private static final double ALTITUD_LUNAR_MAXIMA_KM = 15_000.0;

    private BuscadorTrayectoriaLunar() {
        // Clase de utilidad.
    }

    private record Candidato(
            double deltaVKmps,
            double epocaHoras,
            double direccionY,
            double direccionZ,
            double distanciaCentroLunaKm,
            double altitudLunarKm,
            double encuentroHoras,
            double minimaAltitudTierraPosteriorKm,
            double reentradaHoras,
            boolean retornoPosterior
    ) {
    }

    /**
     * Ejecuta la búsqueda combinada.
     *
     * @param args argumentos no utilizados
     */
    public static void main(String[] args) {

        double[] deltaVsMps = {
                3220.0,
                3240.0,
                3260.0,
                3280.0,
                3300.0
        };

        double[] epocasSegundos = {
                4140.0,
                4200.0,
                4260.0,
                4320.0,
                4380.0
        };

        double[] componentesY = {
                -0.10,
                -0.05,
                 0.00,
                 0.05,
                 0.10
        };

        double[] componentesZ = {
                -0.20,
                -0.10,
                 0.00,
                 0.10,
                 0.20
        };

        int totalPruebas =
                deltaVsMps.length
                * epocasSegundos.length
                * componentesY.length
                * componentesZ.length;

        int numeroPrueba = 0;
        int sobrevuelosSeguros = 0;

        double radioLunaKm =
                Constants.MOON_EQUATORIAL_RADIUS / 1000.0;

        List<Candidato> candidatos = new ArrayList<>();

        System.out.println(
                "=============================================="
        );
        System.out.println(
                "BÚSQUEDA DE SOBREVUELO Y RETORNO TERRESTRE"
        );
        System.out.println(
                "Pruebas programadas: " + totalPruebas
        );
        System.out.printf(
                "Radio lunar utilizado: %.2f km%n",
                radioLunaKm
        );
        System.out.println(
                "=============================================="
        );

        for (double deltaVMps : deltaVsMps) {
            for (double epocaSegundos : epocasSegundos) {
                for (double direccionY : componentesY) {
                    for (double direccionZ : componentesZ) {

                        numeroPrueba++;

                        try {
                            ParametrosSimulacion parametros =
                                    new ParametrosSimulacion(
                                            185.0,
                                            deltaVMps,
                                            epocaSegundos,
                                            240.0,
                                            1200.0,
                                            1.0,
                                            direccionY,
                                            direccionZ
                                    );

                            ResultadoSimulacion resultado =
                                    MotorOrbital.simular(parametros);

                            /*
                             * Encuentro lunar después de las
                             * primeras doce horas.
                             */
                            PuntoTelemetria encuentro =
                                    resultado.puntos()
                                            .stream()
                                            .filter(
                                                punto ->
                                                        punto
                                                        .tiempoSegundos()
                                                        >= 12.0
                                                        * HORA_SEGUNDOS
                                            )
                                            .min(
                                                Comparator
                                                .comparingDouble(
                                                    PuntoTelemetria
                                                    ::distanciaLunarKm
                                                )
                                            )
                                            .orElseThrow();

                            double distanciaCentroLunaKm =
                                    encuentro.distanciaLunarKm();

                            double altitudLunarKm =
                                    distanciaCentroLunaKm
                                    - radioLunaKm;

                            /*
                             * Se descartan colisiones y encuentros
                             * demasiado alejados de la Luna.
                             */
                            if (
                                    altitudLunarKm
                                    < ALTITUD_LUNAR_MINIMA_KM
                                    || altitudLunarKm
                                    > ALTITUD_LUNAR_MAXIMA_KM
                            ) {
                                continue;
                            }

                            sobrevuelosSeguros++;

                            double encuentroSegundos =
                                    encuentro.tiempoSegundos();

                            double encuentroHoras =
                                    encuentroSegundos
                                    / HORA_SEGUNDOS;

                            /*
                             * Menor altitud terrestre registrada
                             * después del encuentro lunar.
                             */
                            double minimaAltitudTierraKm =
                                    resultado.puntos()
                                            .stream()
                                            .filter(
                                                punto ->
                                                        punto
                                                        .tiempoSegundos()
                                                        > encuentroSegundos
                                            )
                                            .mapToDouble(
                                                PuntoTelemetria
                                                ::altitudKm
                                            )
                                            .min()
                                            .orElse(
                                                Double
                                                .POSITIVE_INFINITY
                                            );

                            double reentradaHoras =
                                    resultado.reentrada()
                                            .filter(
                                                punto ->
                                                        punto
                                                        .tiempoSegundos()
                                                        > encuentroSegundos
                                            )
                                            .map(
                                                punto ->
                                                        punto
                                                        .tiempoSegundos()
                                                        / HORA_SEGUNDOS
                                            )
                                            .orElse(Double.NaN);

                            boolean retornoPosterior =
                                    !Double.isNaN(
                                            reentradaHoras
                                    );

                            Candidato candidato =
                                    new Candidato(
                                            deltaVMps / 1000.0,
                                            epocaSegundos
                                                    / HORA_SEGUNDOS,
                                            direccionY,
                                            direccionZ,
                                            distanciaCentroLunaKm,
                                            altitudLunarKm,
                                            encuentroHoras,
                                            minimaAltitudTierraKm,
                                            reentradaHoras,
                                            retornoPosterior
                                    );

                            candidatos.add(candidato);

                        } catch (Exception excepcion) {
                            System.out.printf(
                                    "Error en prueba %d: %s%n",
                                    numeroPrueba,
                                    excepcion.getMessage()
                            );
                        }

                        if (
                                numeroPrueba % 25 == 0
                                || numeroPrueba == totalPruebas
                        ) {
                            System.out.printf(
                                    "Progreso: %d/%d | "
                                    + "sobrevuelos seguros: %d%n",
                                    numeroPrueba,
                                    totalPruebas,
                                    sobrevuelosSeguros
                            );
                        }
                    }
                }
            }
        }

        /*
         * Se priorizan:
         *
         * 1. Reentradas después del encuentro lunar.
         * 2. Menor altitud terrestre posterior.
         * 3. Menor altitud lunar segura.
         */
        candidatos.sort(
                Comparator
                        .comparingInt(
                            (Candidato candidato) ->
                                    candidato
                                    .retornoPosterior()
                                    ? 0
                                    : 1
                        )
                        .thenComparingDouble(
                            Candidato
                            ::minimaAltitudTierraPosteriorKm
                        )
                        .thenComparingDouble(
                            Candidato::altitudLunarKm
                        )
        );

        System.out.println();
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "TOP 20 SOBREVUELOS CON REGRESO MÁS CERCANO"
        );
        System.out.println(
                "=============================================="
        );

        candidatos.stream()
                .limit(20)
                .forEach(candidato -> {

                    String textoReentrada =
                            Double.isNaN(
                                    candidato.reentradaHoras()
                            )
                            ? "no"
                            : String.format(
                                    "%.2f h",
                                    candidato.reentradaHoras()
                            );

                    System.out.printf(
                            "Retorno: %-5s | "
                            + "Alt. Tierra: %10.2f km | "
                            + "Alt. Luna: %8.2f km | "
                            + "Encuentro: %6.2f h | "
                            + "dv: %.3f km/s | "
                            + "Época: %.4f h | "
                            + "Dir: [1.00, %+.2f, %+.2f] | "
                            + "Reentrada: %s%n",
                            candidato.retornoPosterior(),
                            candidato
                            .minimaAltitudTierraPosteriorKm(),
                            candidato.altitudLunarKm(),
                            candidato.encuentroHoras(),
                            candidato.deltaVKmps(),
                            candidato.epocaHoras(),
                            candidato.direccionY(),
                            candidato.direccionZ(),
                            textoReentrada
                    );
                });

        long cantidadRetornos =
                candidatos.stream()
                        .filter(
                            Candidato::retornoPosterior
                        )
                        .count();

        System.out.println();
        System.out.println(
                "Sobrevuelos seguros encontrados: "
                + candidatos.size()
        );

        System.out.println(
                "Retornos con reentrada encontrados: "
                + cantidadRetornos
        );

        System.out.println(
                "=============================================="
        );
    }
}
