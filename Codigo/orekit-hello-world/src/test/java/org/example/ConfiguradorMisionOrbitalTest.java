package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

@DisplayName("OAM-2 a OAM-4 · Componentes físicos de la misión")
class ConfiguradorMisionOrbitalTest {

    @BeforeAll
    static void configurarDatos() {
        ConfiguracionOrekit.configurar();
    }

    @Test
    @DisplayName("OAM-2: crea órbita circular a aproximadamente 185 km")
    void creaOrbitaEstacionamientoCircular() {
        ParametrosSimulacion parametros =
                ParametrosSimulacion.valoresPredeterminados();
        Frame marco = FramesFactory.getGCRF();
        AbsoluteDate fecha = new AbsoluteDate(
                2026, 1, 1, 0, 0, 0.0,
                TimeScalesFactory.getUTC()
        );

        Orbit orbita = ConfiguradorMisionOrbital.crearOrbitaInicial(
                parametros,
                marco,
                fecha
        );

        double altitudKm = (
                orbita.getA()
                - Constants.WGS84_EARTH_EQUATORIAL_RADIUS
        ) / 1000.0;

        assertEquals(185.0, altitudKm, 0.001);
        assertEquals(0.0, orbita.getE(), 1.0e-12);
    }

    @Test
    @DisplayName("OAM-3: registra gravedad 8x8, Luna y Sol")
    void registraTresModelosDeFuerzaRequeridos() {
        ParametrosSimulacion parametros =
                ParametrosSimulacion.valoresPredeterminados();
        Frame inercial = FramesFactory.getGCRF();
        Frame terrestre = FramesFactory.getITRF(
                IERSConventions.IERS_2010,
                true
        );
        AbsoluteDate fecha = new AbsoluteDate(
                2026, 1, 1, 0, 0, 0.0,
                TimeScalesFactory.getUTC()
        );
        Orbit orbita = ConfiguradorMisionOrbital.crearOrbitaInicial(
                parametros,
                inercial,
                fecha
        );

        NumericalPropagator propagador =
                ConfiguradorMisionOrbital.crearPropagador(
                        orbita,
                        inercial,
                        terrestre,
                        CelestialBodyFactory.getMoon(),
                        CelestialBodyFactory.getSun()
                );

        List<ForceModel> modelos = propagador.getAllForceModels();
        long tercerosCuerpos = modelos
                .stream()
                .filter(ThirdBodyAttraction.class::isInstance)
                .count();

        assertTrue(
                modelos.stream().anyMatch(
                        HolmesFeatherstoneAttractionModel.class::isInstance
                )
        );
        assertEquals(2L, tercerosCuerpos);

        NormalizedSphericalHarmonicsProvider gravedad =
                ConfiguradorMisionOrbital.crearGravedad8x8();
        assertEquals(8, gravedad.getMaxDegree());
        assertEquals(8, gravedad.getMaxOrder());
    }

    @ParameterizedTest(name = "delta-v configurado = {0} m/s")
    @ValueSource(doubles = {1000.0, 2000.0, 3220.0})
    @DisplayName("OAM-4: conserva magnitud y dirección de la TLI")
    void creaDeltaVConMagnitudYDireccionCorrectas(double deltaV) {
        ParametrosSimulacion parametros = new ParametrosSimulacion(
                185.0,
                deltaV,
                4320.0,
                72.0,
                600.0,
                1.0,
                0.10,
                -0.20
        );

        Vector3D vector =
                ConfiguradorMisionOrbital.crearVectorDeltaV(parametros);
        Vector3D direccionEsperada =
                new Vector3D(1.0, 0.10, -0.20).normalize();

        assertEquals(deltaV, vector.getNorm(), 1.0e-9);
        assertEquals(1.0, Vector3D.dotProduct(
                vector.normalize(),
                direccionEsperada
        ), 1.0e-12);

        Vector3D velocidadAntes = new Vector3D(0.0, 7700.0, 0.0);
        Vector3D velocidadDespues = velocidadAntes.add(vector);
        assertEquals(
                deltaV,
                velocidadDespues.subtract(velocidadAntes).getNorm(),
                1.0e-9
        );
    }
}
