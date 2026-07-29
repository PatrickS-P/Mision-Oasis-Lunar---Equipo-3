package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.ExtremumApproachDetector;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Motor de propagación numérica de la Misión Oasis Lunar.
 *
 * Incluye gravedad terrestre 8x8, perturbaciones de Luna y Sol,
 * maniobra TLI, controlador de pasos y detectores de eventos.
 */
public final class MotorOrbital {

    private static final double ALTITUD_REENTRADA_M = 120_000.0;
    private static final double TOLERANCIA_TIEMPO_SEGUNDOS = 1.0e-6;

    private MotorOrbital() {
        // Clase de utilidad.
    }

    /**
     * Ejecuta una simulación orbital completa.
     *
     * @param parametros parámetros configurables de la misión
     * @return trayectoria, periapsis lunar y posible reentrada
     */
    public static ResultadoSimulacion simular(
            ParametrosSimulacion parametros
    ) {
        String fuenteDatos = ConfiguracionOrekit.configurar();

        Frame marcoInercial = FramesFactory.getGCRF();
        Frame marcoTerrestre = FramesFactory.getITRF(
                IERSConventions.IERS_2010,
                true
        );

        OneAxisEllipsoid tierra =
                ConfiguradorMisionOrbital.crearTierra(marcoTerrestre);

        AbsoluteDate fechaInicial = new AbsoluteDate(
                2026,
                1,
                1,
                0,
                0,
                0.0,
                TimeScalesFactory.getUTC()
        );

        Orbit orbitaInicial =
                ConfiguradorMisionOrbital.crearOrbitaInicial(
                        parametros,
                        marcoInercial,
                        fechaInicial
                );

        CelestialBody luna = CelestialBodyFactory.getMoon();
        CelestialBody sol = CelestialBodyFactory.getSun();

        NumericalPropagator propagador =
                ConfiguradorMisionOrbital.crearPropagador(
                        orbitaInicial,
                        marcoInercial,
                        marcoTerrestre,
                        luna,
                        sol
                );

        AbsoluteDate fechaTli = fechaInicial.shiftedBy(
                parametros.retrasoTliSegundos()
        );

        propagador.addEventDetector(
                ConfiguradorMisionOrbital.crearManiobraTli(
                        parametros,
                        fechaInicial
                )
        );

        /* OAM-6: detector del acercamiento extremo a la Luna. */
        RecordAndContinue registroPeriapsis = new RecordAndContinue();
        ExtremumApproachDetector detectorPeriapsis =
                new ExtremumApproachDetector(luna)
                        .withMaxCheck(1_800.0)
                        .withThreshold(1.0)
                        .withHandler(registroPeriapsis);
        propagador.addEventDetector(detectorPeriapsis);

        /* OAM-7: cruce descendente de la interfaz de reentrada a 120 km. */
        RecordAndContinue registroReentrada = new RecordAndContinue();
        AltitudeDetector detectorReentrada =
                new AltitudeDetector(
                        600.0,
                        1.0,
                        ALTITUD_REENTRADA_M,
                        tierra
                ).withHandler(registroReentrada);
        propagador.addEventDetector(detectorReentrada);

        double duracionSegundos = parametros.duracionHoras() * 3600.0;
        int cantidadPuntos = Math.max(
                500,
                (int) Math.ceil(
                        duracionSegundos
                        / parametros.pasoMuestreoSegundos()
                ) + 1
        );
        double pasoReal = duracionSegundos / (cantidadPuntos - 1);

        List<PuntoTelemetria> puntosCapturados = new ArrayList<>();

        /*
         * OAM-5: se recopila la trayectoria en una sola propagación.
         * Solamente se registran puntos a partir de la TLI.
         */
        OrekitStepNormalizer controladorPasos =
                new OrekitStepNormalizer(
                        pasoReal,
                        estado -> {
                            if (estado.getDate().compareTo(fechaTli) >= 0) {
                                puntosCapturados.add(
                                        convertirEstado(
                                                estado,
                                                fechaInicial,
                                                luna,
                                                marcoInercial,
                                                tierra
                                        )
                                );
                            }
                        }
                );

        propagador.getMultiplexer().add(controladorPasos);

        AbsoluteDate fechaFinal =
                fechaInicial.shiftedBy(duracionSegundos);
        propagador.propagate(fechaFinal);

        Optional<SpacecraftState> estadoPeriapsis =
                registroPeriapsis
                        .getEvents()
                        .stream()
                        .filter(RecordAndContinue.Event::isIncreasing)
                        .map(RecordAndContinue.Event::getState)
                        .filter(
                                estado ->
                                        estado.getDate()
                                                .compareTo(fechaTli) >= 0
                        )
                        .min(
                                Comparator.comparingDouble(
                                        estado -> distanciaLunar(
                                                estado,
                                                luna,
                                                marcoInercial
                                        )
                                )
                        );

        Optional<SpacecraftState> estadoReentrada =
                registroReentrada
                        .getEvents()
                        .stream()
                        .filter(evento -> !evento.isIncreasing())
                        .map(RecordAndContinue.Event::getState)
                        .filter(
                                estado ->
                                        estado.getDate()
                                                .compareTo(fechaTli) >= 0
                        )
                        .findFirst();

        estadoPeriapsis.ifPresent(
                estado -> puntosCapturados.add(
                        convertirEstado(
                                estado,
                                fechaInicial,
                                luna,
                                marcoInercial,
                                tierra
                        )
                )
        );

        estadoReentrada.ifPresent(
                estado -> puntosCapturados.add(
                        convertirEstado(
                                estado,
                                fechaInicial,
                                luna,
                                marcoInercial,
                                tierra
                        )
                )
        );

        List<PuntoTelemetria> puntos = ordenarYDepurar(puntosCapturados);

        /*
         * La trayectoria del MVS termina en la interfaz de reentrada.
         * Esto evita que la interfaz continúe animando la nave después
         * de detectar el retorno terrestre.
         */
        if (estadoReentrada.isPresent()) {
            double tiempoReentrada = estadoReentrada.get()
                    .getDate()
                    .durationFrom(fechaInicial);

            puntos = puntos
                    .stream()
                    .filter(
                            punto ->
                                    punto.tiempoSegundos()
                                    <= tiempoReentrada
                                    + TOLERANCIA_TIEMPO_SEGUNDOS
                    )
                    .toList();
        }

        if (puntos.isEmpty()) {
            throw new IllegalStateException(
                    "Orekit no generó puntos de trayectoria."
            );
        }

        /*
         * La lista pudo haberse reasignado al recortarla en la reentrada.
         * Guardamos la versión definitiva en una referencia final para que
         * Java permita usarla dentro de las expresiones lambda siguientes.
         */
        final List<PuntoTelemetria> puntosFinales = puntos;

        int indicePeriapsis = estadoPeriapsis
                .map(
                        estado -> buscarIndiceTemporal(
                                puntosFinales,
                                estado.getDate().durationFrom(fechaInicial)
                        )
                )
                .orElseGet(
                        () -> buscarDistanciaLunarMinima(puntosFinales)
                );

        int indiceReentrada = estadoReentrada
                .map(
                        estado -> buscarIndiceTemporal(
                                puntosFinales,
                                estado.getDate().durationFrom(fechaInicial)
                        )
                )
                .orElse(-1);

        return new ResultadoSimulacion(
                puntosFinales,
                indicePeriapsis,
                indiceReentrada,
                fuenteDatos
        );
    }

    private static PuntoTelemetria convertirEstado(
            SpacecraftState estado,
            AbsoluteDate fechaInicial,
            CelestialBody luna,
            Frame marcoInercial,
            OneAxisEllipsoid tierra
    ) {
        PVCoordinates pv = estado.getPVCoordinates();
        Vector3D posicion = pv.getPosition();
        Vector3D posicionLuna = luna.getPVCoordinates(
                estado.getDate(),
                marcoInercial
        ).getPosition();

        double altitudMetros = tierra.transform(
                posicion,
                marcoInercial,
                estado.getDate()
        ).getAltitude();

        EstadoTelemetria estadoTelemetria = new EstadoTelemetria() {
            @Override
            public Vector3D posicionNaveMetros() {
                return posicion;
            }

            @Override
            public Vector3D posicionLunaMetros() {
                return posicionLuna;
            }

            @Override
            public Vector3D velocidadNaveMetrosSegundo() {
                return pv.getVelocity();
            }

            @Override
            public double altitudTerrestreMetros() {
                return altitudMetros;
            }
        };

        return ModeloTelemetria.calcular(
                estado.getDate().durationFrom(fechaInicial),
                estadoTelemetria
        );
    }

    private static double distanciaLunar(
            SpacecraftState estado,
            CelestialBody luna,
            Frame marcoInercial
    ) {
        Vector3D posicionNave =
                estado.getPVCoordinates().getPosition();
        Vector3D posicionLuna = luna.getPVCoordinates(
                estado.getDate(),
                marcoInercial
        ).getPosition();

        return Vector3D.distance(posicionNave, posicionLuna);
    }

    private static List<PuntoTelemetria> ordenarYDepurar(
            List<PuntoTelemetria> puntosOriginales
    ) {
        List<PuntoTelemetria> ordenados = new ArrayList<>(puntosOriginales);
        ordenados.sort(
                Comparator.comparingDouble(
                        PuntoTelemetria::tiempoSegundos
                )
        );

        List<PuntoTelemetria> depurados = new ArrayList<>();

        for (PuntoTelemetria punto : ordenados) {
            if (depurados.isEmpty()) {
                depurados.add(punto);
                continue;
            }

            PuntoTelemetria ultimo = depurados.get(depurados.size() - 1);
            double diferencia = Math.abs(
                    punto.tiempoSegundos() - ultimo.tiempoSegundos()
            );

            if (diferencia <= TOLERANCIA_TIEMPO_SEGUNDOS) {
                /* Se conserva el último, que suele ser el punto exacto del evento. */
                depurados.set(depurados.size() - 1, punto);
            } else {
                depurados.add(punto);
            }
        }

        return depurados;
    }

    private static int buscarIndiceTemporal(
            List<PuntoTelemetria> puntos,
            double tiempoSegundos
    ) {
        int mejorIndice = 0;
        double mejorDiferencia = Double.POSITIVE_INFINITY;

        for (int indice = 0; indice < puntos.size(); indice++) {
            double diferencia = Math.abs(
                    puntos.get(indice).tiempoSegundos()
                    - tiempoSegundos
            );

            if (diferencia < mejorDiferencia) {
                mejorDiferencia = diferencia;
                mejorIndice = indice;
            }
        }

        return mejorIndice;
    }

    private static int buscarDistanciaLunarMinima(
            List<PuntoTelemetria> puntos
    ) {
        int mejorIndice = 0;
        double menorDistancia = Double.POSITIVE_INFINITY;

        for (int indice = 0; indice < puntos.size(); indice++) {
            double distancia = puntos.get(indice).distanciaLunarKm();

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejorIndice = indice;
            }
        }

        return mejorIndice;
    }

    /**
     * Ejecuta una prueba del motor orbital.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        ResultadoSimulacion resultado = simular(
                ParametrosSimulacion.valoresPredeterminados()
        );

        PuntoTelemetria periapsis = resultado.periapsisLunar();

        System.out.println("========================================");
        System.out.println("MISIÓN OASIS LUNAR - MOTOR NUMÉRICO");
        System.out.println("========================================");
        System.out.println(
                "Puntos generados con StepHandler: "
                + resultado.puntos().size()
        );
        System.out.printf(
                "Altitud del periapsis lunar: %.2f km%n",
                resultado.altitudPeriapsisLunarKm()
        );
        System.out.printf(
                "Tiempo del periapsis: %.2f horas%n",
                periapsis.tiempoSegundos() / 3600.0
        );
        System.out.println(
                "Reentrada detectada por evento: "
                + resultado.reentrada().isPresent()
        );
        System.out.println("Datos Orekit: " + resultado.fuenteDatos());
        System.out.println("========================================");
    }
}
