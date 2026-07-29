package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.potential.GravityFieldFactory;

@DisplayName("OAM-1 · Configuración de datos Orekit")
class ConfiguracionOrekitTest {

    @Test
    @DisplayName("inicializa el contexto, registra proveedor y carga una fuente")
    void inicializaContextoOrekit() {
        String ruta = assertDoesNotThrow(
                ConfiguracionOrekit::configurar
        );

        assertTrue(Files.isDirectory(Path.of(ruta)));
        assertFalse(
                DataContext.getDefault()
                        .getDataProvidersManager()
                        .getProviders()
                        .isEmpty(),
                "Debe existir al menos un proveedor de datos registrado."
        );

        assertDoesNotThrow(
                () -> GravityFieldFactory.getNormalizedProvider(2, 0)
        );

        assertFalse(
                DataContext.getDefault()
                        .getDataProvidersManager()
                        .getLoadedDataNames()
                        .isEmpty(),
                "Orekit debe haber cargado al menos un archivo de datos."
        );
    }
}
