package org.example;

import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/**
 * Prototipo de consola del motor orbital de la Misión Oasis Lunar.
 * Configura una órbita terrestre baja mediante Orekit.
 */
public class LeoHello {

    /**
     * Punto de entrada del prototipo orbital en consola.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {

        System.out.println("==============================================================");
        System.out.println("   MISIÓN OASIS LUNAR - PRUEBA DEL MOTOR ORBITAL OREKIT");
        System.out.println("==============================================================");

        Frame frame = FramesFactory.getGCRF();
        AbsoluteDate fechaInicial = AbsoluteDate.J2000_EPOCH;

        double radioTierra = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double altitud = 400_000.0;
        double semiejeMayor = radioTierra + altitud;

        double excentricidad = 0.001;
        double inclinacion = FastMath.toRadians(51.6);
        double argumentoPerigeo = 0.0;
        double raan = 0.0;
        double anomaliaMedia = 0.0;
        double muTierra = Constants.WGS84_EARTH_MU;

        Orbit orbitaInicial = new KeplerianOrbit(
                semiejeMayor,
                excentricidad,
                inclinacion,
                argumentoPerigeo,
                raan,
                anomaliaMedia,
                PositionAngleType.MEAN,
                frame,
                fechaInicial,
                muTierra
        );

        KeplerianPropagator propagador =
                new KeplerianPropagator(orbitaInicial);

        double periodo = orbitaInicial.getKeplerianPeriod();

        System.out.printf("Altitud inicial: %.2f km%n", altitud / 1000.0);
        System.out.printf("Inclinación: %.2f grados%n", FastMath.toDegrees(inclinacion));
        System.out.printf("Periodo orbital: %.2f minutos%n%n", periodo / 60.0);

        System.out.println("Paso | Tiempo (min) | X (km)     | Y (km)     | Z (km)     | Velocidad (km/s)");
        System.out.println("--------------------------------------------------------------------------------");

        int paso = 0;

        for (double tiempo = 0; tiempo <= periodo; tiempo += 300.0) {

            AbsoluteDate fecha = fechaInicial.shiftedBy(tiempo);
            SpacecraftState estado = propagador.propagate(fecha);
            PVCoordinates coordenadas = estado.getPVCoordinates();

            double x = coordenadas.getPosition().getX() / 1000.0;
            double y = coordenadas.getPosition().getY() / 1000.0;
            double z = coordenadas.getPosition().getZ() / 1000.0;
            double velocidad = coordenadas.getVelocity().getNorm() / 1000.0;

            System.out.printf(
                    "%4d | %12.2f | %10.2f | %10.2f | %10.2f | %16.3f%n",
                    paso,
                    tiempo / 60.0,
                    x,
                    y,
                    z,
                    velocidad
            );

            paso++;
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Propagación orbital completada correctamente.");
        System.out.println("Orekit funciona correctamente en el proyecto.");
        System.out.println("==============================================================");
    }
}
