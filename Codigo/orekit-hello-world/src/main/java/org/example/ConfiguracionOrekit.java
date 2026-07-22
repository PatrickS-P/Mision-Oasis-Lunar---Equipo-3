package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

/**
 * Localiza y registra los datos auxiliares utilizados por Orekit.
 */
public final class ConfiguracionOrekit {

    private static boolean configurada;
    private static Path rutaConfigurada;

    private ConfiguracionOrekit() {
        // Clase de utilidad.
    }

    /**
     * Busca una carpeta orekit-data y la registra en Orekit.
     *
     * @return ruta absoluta de la carpeta utilizada
     * @throws IllegalStateException cuando no se encuentra una carpeta válida
     */
    public static synchronized String configurar() {

        if (configurada) {
            return rutaConfigurada.toString();
        }

        List<Path> candidatas = new ArrayList<>();

        String variableEntorno = System.getenv("OREKIT_DATA_DIR");
        if (variableEntorno != null && !variableEntorno.isBlank()) {
            candidatas.add(Path.of(variableEntorno));
        }

        candidatas.add(Path.of("orekit-data"));
        candidatas.add(Path.of("..", "orekit-data"));
        candidatas.add(Path.of(System.getProperty("user.home"), "orekit-data"));

        for (Path candidata : candidatas) {
            Path ruta = candidata.toAbsolutePath().normalize();

            if (Files.isDirectory(ruta)) {
                DataProvidersManager manager =
                        DataContext.getDefault().getDataProvidersManager();

                manager.addProvider(new DirectoryCrawler(ruta.toFile()));

                configurada = true;
                rutaConfigurada = ruta;

                return ruta.toString();
            }
        }

        throw new IllegalStateException(
                "No se encontró la carpeta orekit-data. "
                + "Configure la variable OREKIT_DATA_DIR."
        );
    }
}
