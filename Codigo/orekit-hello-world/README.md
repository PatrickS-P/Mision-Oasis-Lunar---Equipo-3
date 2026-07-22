# Misión Oasis Lunar

Simulador aeroespacial educativo desarrollado por la Tripulación 3 con Java, Orekit y JavaFX.

## Tecnologías

- Java 17
- Maven 3.9+
- Orekit, según la versión declarada en pom.xml
- JavaFX

## Compilar

En macOS, seleccionar Java 17 y compilar:

    export JAVA_HOME=$(/usr/libexec/java_home -v 17)
    mvn clean package

El archivo JAR se genera dentro de la carpeta target.

## Ejecutar la interfaz gráfica

    mvn javafx:run

Clase principal: org.example.TrayectoriaGrafica

## Generar Javadoc

    mvn javadoc:javadoc

La documentación HTML se genera en target/reports/apidocs.

## Alcance actual

El proyecto contiene un prototipo orbital de consola y una interfaz JavaFX que representa la Tierra, la Luna, la nave y su trayectoria. Este prototipo no debe presentarse como la implementación completa de todos los requisitos físicos de la misión.

## Tripulación 3

- Laura Priscila Guerrero — CDR y coordinación
- Patrick — arquitectura y Git
- Rommel — física y Orekit
- Yanantonys — requerimientos
- Martín — documentación

Las dependencias se administran mediante Maven. No deben agregarse archivos JAR manualmente ni subirse target, .vscode o datos locales de Orekit.
