package org.example;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
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
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * Motor de propagación numérica de la Misión Oasis Lunar.
 *
 * Incluye gravedad terrestre 8x8, perturbaciones de Luna y Sol,
 * maniobra impulsiva TLI y generación de telemetría.
 */
public final class MotorOrbital {

    private static final double MASA_INICIAL_KG = 1_000.0;
    private static final double ISP_SEGUNDOS = 450.0;
    private static final double ALTITUD_REENTRADA_KM = 120.0;

    private MotorOrbital() {
        // Clase de utilidad.
    }

    /**
     * Ejecuta una simulación orbital completa.
     *
     * @param parametros parámetros configurables de la misión
     * @return resultado con trayectoria, periapsis y posible reentrada
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
                0.001,
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

        ImpulseManeuver maniobraTli =
                new ImpulseManeuver(
                        disparadorTli,
                        new Vector3D(
                                parametros.deltaVMps(),
                                0.0,
                                0.0
                        ),
                        ISP_SEGUNDOS
                );

        propagador.addEventDetector(maniobraTli);

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

        List<PuntoTelemetria> puntos = new ArrayList<>();

        int indicePeriapsis = 0;
        int indiceReentrada = -1;
        double distanciaLunarMinima = Double.POSITIVE_INFINITY;

        for (int indice = 0; indice < cantidadPuntos; indice++) {

            double tiempo = indice * pasoReal;
            AbsoluteDate fecha = fechaInicial.shiftedBy(tiempo);

            SpacecraftState estado =
                    propagador.propagate(fecha);

            PVCoordinates pv = estado.getPVCoordinates();

            Vector3D posicion = pv.getPosition();
            Vector3D posicionLuna =
                    luna.getPVCoordinates(
                            fecha,
                            marcoInercial
                    ).getPosition();

            double altitudKm =
                    (
                            posicion.getNorm()
                            - Constants.WGS84_EARTH_EQUATORIAL_RADIUS
                    ) / 1000.0;

            double distanciaLunarKm =
                    Vector3D.distance(
                            posicion,
                            posicionLuna
                    ) / 1000.0;

            double velocidadKmS =
                    pv.getVelocity().getNorm() / 1000.0;

            PuntoTelemetria punto = new PuntoTelemetria(
                    tiempo,
                    posicion.getX() / 1000.0,
                    posicion.getY() / 1000.0,
                    posicion.getZ() / 1000.0,
                    altitudKm,
                    distanciaLunarKm,
                    velocidadKmS
            );

            puntos.add(punto);

            if (distanciaLunarKm < distanciaLunarMinima) {
                distanciaLunarMinima = distanciaLunarKm;
                indicePeriapsis = indice;
            }

            if (
                    indiceReentrada < 0
                    && altitudKm <= ALTITUD_REENTRADA_KM
            ) {
                indiceReentrada = indice;
            }
        }

        return new ResultadoSimulacion(
                puntos,
                indicePeriapsis,
                indiceReentrada,
                fuenteDatos
        );
    }

    /**
     * Ejecuta una prueba rápida del motor orbital.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {

        ResultadoSimulacion resultado = simular(
                ParametrosSimulacion.valoresPredeterminados()
        );

        PuntoTelemetria periapsis =
                resultado.periapsisLunar();

        System.out.println("========================================");
        System.out.println("MISIÓN OASIS LUNAR - MOTOR NUMÉRICO");
        System.out.println("========================================");
        System.out.println(
                "Puntos generados: "
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
                "Reentrada detectada: "
                + resultado.reentrada().isPresent()
        );
        System.out.println(
                "Datos Orekit: "
                + resultado.fuenteDatos()
        );
        System.out.println("========================================");
    }
}
