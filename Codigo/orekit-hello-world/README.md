# Orekit LEO Hello World

Propagación kepleriana de una órbita LEO circular (estilo ISS) en Java con Orekit.

## Requisitos
- Java 11+  (`java -version`)
- Maven 3.6+  (`mvn -version`)
- Archivo de datos Orekit (ver paso 2)

## Paso 1 — Compilar y empaquetar

```bash
mvn package -q
```

Genera `target/orekit-hello-1.0-SNAPSHOT-shaded.jar` (fat-jar con todo incluido).

## Paso 2 — Datos de Orekit (OBLIGATORIO)

Orekit necesita archivos de orientación terrestre (EOP), UTC-TAI, etc.

### Opción A — jar de datos oficial (más fácil)

```bash
# Descarga el jar de datos (~60 MB) desde Orekit Gitlab
curl -LO https://gitlab.orekit.org/orekit/orekit-data/-/archive/main/orekit-data-main.zip
unzip orekit-data-main.zip -d .
mv orekit-data-main orekit-data
```

### Opción B — Variable de entorno

```bash
export OREKIT_DATA_DIR=/ruta/a/tu/orekit-data
```

## Paso 3 — Ejecutar

```bash
java -jar target/orekit-hello-1.0-SNAPSHOT-shaded.jar
```

## Salida esperada

```
✓ Datos Orekit cargados desde: .../orekit-data
✓ Época inicial : 2024-01-01T00:00:00.000
✓ Período orbital : 92.45 min

Fecha UTC                      X [km]         Y [km]         Z [km]    Radio [km]
──────────────────────────────────────────────────────────────────────────────────────────
2024-01-01T00:00:00.000        6778.000         0.000         0.000      6778.000
2024-01-01T00:05:00.000        6611.234      1273.456       820.123      6778.001
...
✓ Propagación completada — cadena de herramientas Orekit OK
```

## Parámetros orbitales usados

| Parámetro | Valor | Descripción |
|-----------|-------|-------------|
| a | 6 778 km | Semieje mayor (altitud ~400 km) |
| e | 0.001 | Excentricidad (casi circular) |
| i | 51.6° | Inclinación (igual que la ISS) |
| Ω, ω, M₀ | 0° | RAAN, arg. perigeo, anomalía media |
| Propagador | Kepleriano | Solo J2 implícito en μ |
| Duración | ~92 min | 1 período completo, paso 5 min |

## Próximos pasos

- Cambiar a `NumericalPropagator` para incluir perturbaciones (J2, drag, SRP)
- Agregar `EcksteinHechlerPropagator` para perturbaciones analíticas rápidas
- Exportar a TLE con `TleBuilder`
- Graficar la traza en tierra con `OneAxisEllipsoid` + `TopocentricFrame`
