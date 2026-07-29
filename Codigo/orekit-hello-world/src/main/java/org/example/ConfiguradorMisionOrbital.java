package org.example;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
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
import org.orekit.utils.Constants;

/**
 * Construye los componentes físicos principales de la misión.
 *
 * La separación permite verificar de forma unitaria la órbita inicial,
 * los modelos de fuerza y la configuración de la maniobra TLI.
 */
public final class ConfiguradorMisionOrbital {

    public static final double MASA_INICIAL_KG = 1_000.0;
    public static final double ISP_SEGUNDOS = 450.0;

    private ConfiguradorMisionOrbital() {
        // Clase de utilidad.
    }

    /**
     * Crea la órbita circular de estacionamiento terrestre.
     *
     * @param parametros parámetros de la misión
     * @param marcoInercial marco inercial de propagación
     * @param fechaInicial fecha inicial
     * @return órbita circular de estacionamiento
     */
    public static Orbit crearOrbitaInicial(
            ParametrosSimulacion parametros,
            Frame marcoInercial,
            AbsoluteDate fechaInicial
    ) {
        double radioInicial =
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS
                + parametros.altitudInicialKm() * 1000.0;

        return new KeplerianOrbit(
                radioInicial,
                0.0,
                Math.toRadians(28.5),
                0.0,
                0.0,
                0.0,
                PositionAngleType.MEAN,
                marcoInercial,
                fechaInicial,
                Constants.WGS84_EARTH_MU
        );
    }

    /**
     * Obtiene el campo de gravedad terrestre normalizado 8x8.
     *
     * @return proveedor de armónicos esféricos 8x8
     */
    public static NormalizedSphericalHarmonicsProvider crearGravedad8x8() {
        return GravityFieldFactory.getNormalizedProvider(8, 8);
    }

    /**
     * Crea el propagador numérico y registra los tres modelos de fuerza
     * requeridos: gravedad terrestre 8x8, Luna y Sol.
     *
     * @param orbitaInicial órbita de estacionamiento
     * @param marcoInercial marco inercial
     * @param marcoTerrestre marco ligado a la Tierra
     * @param luna cuerpo lunar
     * @param sol cuerpo solar
     * @return propagador configurado
     */
    public static NumericalPropagator crearPropagador(
            Orbit orbitaInicial,
            Frame marcoInercial,
            Frame marcoTerrestre,
            CelestialBody luna,
            CelestialBody sol
    ) {
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

        propagador.addForceModel(
                new HolmesFeatherstoneAttractionModel(
                        marcoTerrestre,
                        crearGravedad8x8()
                )
        );
        propagador.addForceModel(new ThirdBodyAttraction(luna));
        propagador.addForceModel(new ThirdBodyAttraction(sol));

        return propagador;
    }

    /**
     * Convierte la magnitud y dirección configuradas en un vector delta-v
     * expresado en el marco local TNW.
     *
     * @param parametros parámetros de la misión
     * @return vector delta-v en metros por segundo
     */
    public static Vector3D crearVectorDeltaV(
            ParametrosSimulacion parametros
    ) {
        Vector3D direccion = new Vector3D(
                parametros.direccionTliX(),
                parametros.direccionTliY(),
                parametros.direccionTliZ()
        ).normalize();

        return direccion.scalarMultiply(parametros.deltaVMps());
    }

    /**
     * Crea la maniobra impulsiva TLI en la fecha configurada.
     *
     * @param parametros parámetros de la misión
     * @param fechaInicial fecha de inicio
     * @return detector/maniobra TLI
     */
    public static ImpulseManeuver crearManiobraTli(
            ParametrosSimulacion parametros,
            AbsoluteDate fechaInicial
    ) {
        AbsoluteDate fechaTli = fechaInicial.shiftedBy(
                parametros.retrasoTliSegundos()
        );

        return new ImpulseManeuver(
                new DateDetector(fechaTli),
                crearVectorDeltaV(parametros),
                ISP_SEGUNDOS
        );
    }

    /**
     * Crea el modelo elipsoidal WGS84 de la Tierra.
     *
     * @param marcoTerrestre marco ligado a la Tierra
     * @return elipsoide terrestre
     */
    public static OneAxisEllipsoid crearTierra(Frame marcoTerrestre) {
        return new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                marcoTerrestre
        );
    }
}
