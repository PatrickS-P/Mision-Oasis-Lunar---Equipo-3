package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.ExtremumApproachDetector;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Motor de propagación numérica de la Misión Oasis Lunar.
 *
 * Incluye gravedad terrestre 8x8, perturbaciones de Luna y Sol,
 * maniobra TLI, controlador de pasos y detectores de eventos.
 */
public final class MotorOrbital {

    private static final double MASA_INICIAL_KG = 1_000.0;
    private static final double ISP_SEGUNDOS = 450.0;
    private static final double ALTITUD_REENTRADA_M = 120_000.0;

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

        OneAxisEllipsoid tierra = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                marcoTerrestre
        );

        AbsoluteDate fechaInicial = new AbsoluteDate(
                2026,
                1,
                1,
                0,
                0,
                0.0,
                TimeScalesFactory.getUTC()
        );

        double radioInicial =
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS
                + parametros.altitudInicialKm() * 1000.0;

        Orbit orbitaInicial = new KeplerianOrbit(
                radioInicial,
                0.0,
                FastMath.toRadians(28.5),
                0.0,
                0.0,
                0.0,
                PositionAngleType.MEAN,
                marcoInercial,
                fechaInicial,
                Constants.WGS84_EARTH_MU
        );

        double[][] tolerancias =
                ToleranceProvider
                        .getDefaultToleranceProvider(10.0)
                        .getTolerances(
                                orbitaInicial,
                                OrbitType.CARTESIAN
                        );

        DormandPrince853Integrator integrador =
                new DormandPrince853Integrator(
                        0.1,
                        300.0,
                        tolerancias[0],
                        tolerancias[1]
                );

        integrador.setInitialStepSize(10.0);

        LofOffset actitudTangencial =
                new LofOffset(marcoInercial, LOFType.TNW);

        NumericalPropagator propagador =
                new NumericalPropagator(
                        integrador,
                        actitudTangencial
                );

        propagador.setOrbitType(OrbitType.CARTESIAN);

        propagador.setInitialState(
                new SpacecraftState(
                        orbitaInicial,
                        MASA_INICIAL_KG
                )
        );

        NormalizedSphericalHarmonicsProvider gravedad8x8 =
                GravityFieldFactory.getNormalizedProvider(8, 8);

        propagador.addForceModel(
                new HolmesFeatherstoneAttractionModel(
                        marcoTerrestre,
                        gravedad8x8
                )
        );

        CelestialBody luna = CelestialBodyFactory.getMoon();
        CelestialBody sol = CelestialBodyFactory.getSun();

        propagador.addForceModel(
                new ThirdBodyAttraction(luna)
        );

        propagador.addForceModel(
                new ThirdBodyAttraction(sol)
        );

        AbsoluteDate fechaTli = fechaInicial.shiftedBy(
                parametros.retrasoTliSegundos()
        );

        DateDetector disparadorTli =
                new DateDetector(fechaTli);

        /*
         * La dirección introducida puede tener cualquier magnitud.
         * Se normaliza antes de multiplicarla por el delta-v.
         *
         * El sistema de referencia local utilizado es TNW:
         * X = tangencial, Y = normal, Z = binormal.
         */
        Vector3D direccionTli = new Vector3D(
                parametros.direccionTliX(),
                parametros.direccionTliY(),
                parametros.direccionTliZ()
        ).normalize();

        Vector3D vectorDeltaV = direccionTli.scalarMultiply(
                parametros.deltaVMps()
        );

        ImpulseManeuver maniobraTli =
                new ImpulseManeuver(
                        disparadorTli,
                        vectorDeltaV,
                        ISP_SEGUNDOS
                );

        propagador.addEventDetector(maniobraTli);

        /*
         * OAM-6: detector del punto de acercamiento extremo
         * entre la nave y la Luna.
         */
        RecordAndContinue registroPeriapsis =
                new RecordAndContinue();

        ExtremumApproachDetector detectorPeriapsis =
                new ExtremumApproachDetector(luna)
                        .withMaxCheck(1_800.0)
                        .withThreshold(1.0)
                        .withHandler(registroPeriapsis);

        propagador.addEventDetector(detectorPeriapsis);

        /*
         * OAM-7: detector del cruce de la interfaz
         * terrestre de reentrada a 120 km.
         */
        RecordAndContinue registroReentrada =
                new RecordAndContinue();

        AltitudeDetector detectorReentrada =
                new AltitudeDetector(
                        600.0,
                        1.0,
                        ALTITUD_REENTRADA_M,
                        tierra
                ).withHandler(registroReentrada);

        propagador.addEventDetector(detectorReentrada);

        double duracionSegundos =
                parametros.duracionHoras() * 3600.0;

        int cantidadPuntos = Math.max(
                500,
                (int) Math.ceil(
                        duracionSegundos
                        / parametros.pasoMuestreoSegundos()
                ) + 1
        );

        double pasoReal =
                duracionSegundos / (cantidadPuntos - 1);

        List<PuntoTelemetria> puntos =
                new ArrayList<>();

        /*
         * OAM-5: OrekitStepNormalizer implementa
         * OrekitStepHandler y recopila la trayectoria
         * durante una sola propagación.
         */
        OrekitStepNormalizer controladorPasos =
                new OrekitStepNormalizer(
                        pasoReal,
                        estado -> {
                            if (
                                    estado.getDate()
                                            .compareTo(fechaTli) >= 0
                            ) {
                                puntos.add(
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

        propagador
                .getMultiplexer()
                .add(controladorPasos);

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
                                        estado ->
                                                distanciaLunar(
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
                estado -> puntos.add(
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
                estado -> puntos.add(
                        convertirEstado(
                                estado,
                                fechaInicial,
                                luna,
                                marcoInercial,
                                tierra
                        )
                )
        );

        puntos.sort(
                Comparator.comparingDouble(
                        PuntoTelemetria::tiempoSegundos
                )
        );

        if (puntos.isEmpty()) {
            throw new IllegalStateException(
                    "Orekit no generó puntos de trayectoria."
            );
        }

        int indicePeriapsis = estadoPeriapsis
                .map(
                        estado -> buscarIndiceTemporal(
                                puntos,
                                estado.getDate()
                                        .durationFrom(fechaInicial)
                        )
                )
                .orElseGet(
                        () -> buscarDistanciaLunarMinima(puntos)
                );

        int indiceReentrada = estadoReentrada
                .map(
                        estado -> buscarIndiceTemporal(
                                puntos,
                                estado.getDate()
                                        .durationFrom(fechaInicial)
                        )
                )
                .orElse(-1);

        return new ResultadoSimulacion(
                puntos,
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

        double altitudKm = tierra.transform(
                posicion,
                marcoInercial,
                estado.getDate()
        ).getAltitude() / 1000.0;

        double distanciaLunarKm = Vector3D.distance(
                posicion,
                posicionLuna
        ) / 1000.0;

        return new PuntoTelemetria(
                estado.getDate().durationFrom(fechaInicial),
                posicion.getX() / 1000.0,
                posicion.getY() / 1000.0,
                posicion.getZ() / 1000.0,
                altitudKm,
                distanciaLunarKm,
                pv.getVelocity().getNorm() / 1000.0
        );
    }

    private static double distanciaLunar(
            SpacecraftState estado,
            CelestialBody luna,
            Frame marcoInercial
    ) {

        Vector3D posicionNave =
                estado.getPVCoordinates().getPosition();

        Vector3D posicionLuna =
                luna.getPVCoordinates(
                        estado.getDate(),
                        marcoInercial
                ).getPosition();

        return Vector3D.distance(
                posicionNave,
                posicionLuna
        );
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

            double distancia =
                    puntos.get(indice).distanciaLunarKm();

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

        PuntoTelemetria periapsis =
                resultado.periapsisLunar();

        System.out.println(
                "========================================"
        );
        System.out.println(
                "MISIÓN OASIS LUNAR - MOTOR NUMÉRICO"
        );
        System.out.println(
                "========================================"
        );

        System.out.println(
                "Puntos generados con StepHandler: "
                + resultado.puntos().size()
        );

        System.out.printf(
                "Distancia lunar mínima: %.2f km%n",
                periapsis.distanciaLunarKm()
        );

        System.out.printf(
                "Tiempo del periapsis: %.2f horas%n",
                periapsis.tiempoSegundos() / 3600.0
        );

        System.out.println(
                "Reentrada detectada por evento: "
                + resultado.reentrada().isPresent()
        );

        System.out.println(
                "Datos Orekit: "
                + resultado.fuenteDatos()
        );

        System.out.println(
                "========================================"
        );
    }
}
