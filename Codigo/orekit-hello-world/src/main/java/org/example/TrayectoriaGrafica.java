package org.example;

import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Interfaz gráfica integrada del simulador Misión Oasis Lunar.
 *
 * Consume la trayectoria precalculada por {@link MotorOrbital},
 * muestra telemetría y permite controlar la reproducción.
 */
public class TrayectoriaGrafica extends Application {

    private final Canvas canvas = new Canvas(850, 600);

    private final TextField campoAltitud = new TextField("185");
    private final TextField campoDeltaV = new TextField("3150");
    private final TextField campoEpocaTli = new TextField("0.1667");

    private final Label estado = new Label("Estado: esperando parámetros");
    private final Label tiempo = new Label("Tiempo: --");
    private final Label altitud = new Label("Altitud terrestre: --");
    private final Label distanciaLuna = new Label("Distancia lunar: --");
    private final Label velocidad = new Label("Velocidad: --");
    private final Label periapsis = new Label("Periapsis lunar: --");

    private final Slider escalaTiempo = new Slider(1, 1000, 100);

    private final Button botonEjecutar = new Button("Ejecutar simulación");
    private final Button botonReiniciar = new Button("Reiniciar");
    private final Button botonReproducir = new Button("Reproducir");
    private final Button botonPausar = new Button("Pausar");

    private ResultadoSimulacion resultado;
    private int indiceActual;
    private boolean reproduciendo;
    private long ultimoInstante;

    /**
     * Construye la escena principal.
     *
     * @param stage ventana principal de JavaFX
     */
    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #07111f;");

        root.setTop(crearEncabezado());
        root.setLeft(crearPanelControl());
        root.setCenter(canvas);
        root.setBottom(crearControlesReproduccion());

        configurarEventos();
        iniciarAnimacion();

        dibujarSinSimulacion();

        Scene scene = new Scene(root, 1180, 720);

        stage.setTitle("Misión Oasis Lunar - Simulador Artemis II");
        stage.setScene(scene);
        stage.show();
    }

    private VBox crearEncabezado() {

        Label titulo = new Label("MISIÓN OASIS LUNAR");
        titulo.setFont(Font.font(25));
        titulo.setTextFill(Color.WHITE);

        Label subtitulo = new Label(
                "Simulación numérica Orekit 13 · Tierra–Luna · Telemetría"
        );
        subtitulo.setTextFill(Color.LIGHTGRAY);

        VBox encabezado = new VBox(4, titulo, subtitulo);
        encabezado.setPadding(new Insets(14));

        return encabezado;
    }

    private VBox crearPanelControl() {

        configurarCampo(campoAltitud);
        configurarCampo(campoDeltaV);
        configurarCampo(campoEpocaTli);

        Label tituloParametros = crearTituloSeccion("PARÁMETROS");

        VBox parametros = new VBox(
                6,
                tituloParametros,
                crearEtiqueta("Altitud inicial (km)"),
                campoAltitud,
                crearEtiqueta("Delta-v TLI (m/s)"),
                campoDeltaV,
                crearEtiqueta("Época TLI (horas)"),
                campoEpocaTli,
                botonEjecutar,
                botonReiniciar
        );

        Label tituloTelemetria = crearTituloSeccion("TELEMETRÍA");

        for (Label etiqueta : List.of(
                estado,
                tiempo,
                altitud,
                distanciaLuna,
                velocidad,
                periapsis
        )) {
            etiqueta.setTextFill(Color.WHITE);
            etiqueta.setWrapText(true);
        }

        VBox telemetria = new VBox(
                7,
                tituloTelemetria,
                estado,
                tiempo,
                altitud,
                distanciaLuna,
                velocidad,
                periapsis
        );

        VBox panel = new VBox(18, parametros, telemetria);
        panel.setPadding(new Insets(15));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: #0d1b2a;");

        return panel;
    }

    private HBox crearControlesReproduccion() {

        Label etiquetaEscala = new Label("Escala temporal:");
        etiquetaEscala.setTextFill(Color.WHITE);

        Label valorEscala = new Label("100x");
        valorEscala.setTextFill(Color.LIGHTGREEN);

        escalaTiempo.setShowTickLabels(true);
        escalaTiempo.setMajorTickUnit(250);
        escalaTiempo.setPrefWidth(260);

        escalaTiempo.valueProperty().addListener(
                (observable, anterior, nuevo) ->
                        valorEscala.setText(
                                String.format("%.0fx", nuevo.doubleValue())
                        )
        );

        HBox controles = new HBox(
                12,
                botonReproducir,
                botonPausar,
                etiquetaEscala,
                escalaTiempo,
                valorEscala
        );

        controles.setAlignment(Pos.CENTER);
        controles.setPadding(new Insets(12));
        controles.setStyle("-fx-background-color: #0d1b2a;");

        return controles;
    }

    private void configurarEventos() {

        botonEjecutar.setOnAction(evento -> ejecutarSimulacion());
        botonReiniciar.setOnAction(evento -> reiniciar());
        botonReproducir.setOnAction(evento -> {
            if (resultado != null) {
                reproduciendo = true;
                estado.setText("Estado: reproducción activa");
            }
        });

        botonPausar.setOnAction(evento -> {
            reproduciendo = false;
            estado.setText("Estado: simulación pausada");
        });
    }

    private void ejecutarSimulacion() {

        ParametrosSimulacion parametros;

        try {
            double altitudKm = Double.parseDouble(campoAltitud.getText());
            double deltaVMps = Double.parseDouble(campoDeltaV.getText());
            double epocaHoras = Double.parseDouble(campoEpocaTli.getText());

            parametros = new ParametrosSimulacion(
                    altitudKm,
                    deltaVMps,
                    epocaHoras * 3600.0,
                    120.0,
                    600.0
            );
        } catch (NumberFormatException excepcion) {
            estado.setText("Error: introduzca solamente valores numéricos.");
            return;
        } catch (IllegalArgumentException excepcion) {
            estado.setText("Error: " + excepcion.getMessage());
            return;
        }

        botonEjecutar.setDisable(true);
        reproduciendo = false;
        estado.setText("Estado: calculando trayectoria con Orekit…");

        Task<ResultadoSimulacion> tarea = new Task<>() {
            @Override
            protected ResultadoSimulacion call() {
                return MotorOrbital.simular(parametros);
            }
        };

        tarea.setOnSucceeded(evento -> {
            resultado = tarea.getValue();
            indiceActual = 0;
            ultimoInstante = 0;
            reproduciendo = true;

            botonEjecutar.setDisable(false);

            PuntoTelemetria puntoPeriapsis =
                    resultado.periapsisLunar();

            periapsis.setText(
                    String.format(
                            "Periapsis lunar: %.2f km a las %.2f h",
                            puntoPeriapsis.distanciaLunarKm(),
                            puntoPeriapsis.tiempoSegundos() / 3600.0
                    )
            );

            estado.setText(
                    "Estado: trayectoria lista · "
                    + resultado.puntos().size()
                    + " puntos"
            );

            dibujar();
        });

        tarea.setOnFailed(evento -> {
            botonEjecutar.setDisable(false);
            estado.setText(
                    "Error de simulación: "
                    + tarea.getException().getMessage()
            );
            tarea.getException().printStackTrace();
        });

        Thread hilo = new Thread(tarea, "motor-orbital-orekit");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void iniciarAnimacion() {

        new AnimationTimer() {
            @Override
            public void handle(long ahora) {

                if (resultado == null || !reproduciendo) {
                    return;
                }

                if (ultimoInstante == 0) {
                    ultimoInstante = ahora;
                    return;
                }

                double segundosReales =
                        (ahora - ultimoInstante) / 1_000_000_000.0;

                double velocidadReproduccion =
                        escalaTiempo.getValue();

                int avance = Math.max(
                        1,
                        (int) Math.round(
                                segundosReales
                                * velocidadReproduccion
                                / 10.0
                        )
                );

                indiceActual += avance;
                ultimoInstante = ahora;

                if (indiceActual >= resultado.puntos().size() - 1) {
                    indiceActual = resultado.puntos().size() - 1;
                    reproduciendo = false;
                    estado.setText("Estado: simulación completada");
                }

                actualizarTelemetria();
                dibujar();
            }
        }.start();
    }

    private void actualizarTelemetria() {

        PuntoTelemetria punto =
                resultado.puntos().get(indiceActual);

        tiempo.setText(
                String.format(
                        "Tiempo: %.2f horas",
                        punto.tiempoSegundos() / 3600.0
                )
        );

        altitud.setText(
                String.format(
                        "Altitud terrestre: %.2f km",
                        punto.altitudKm()
                )
        );

        distanciaLuna.setText(
                String.format(
                        "Distancia lunar: %.2f km",
                        punto.distanciaLunarKm()
                )
        );

        velocidad.setText(
                String.format(
                        "Velocidad: %.3f km/s",
                        punto.velocidadKmS()
                )
        );
    }

    private void dibujar() {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        double ancho = canvas.getWidth();
        double alto = canvas.getHeight();
        double centroX = ancho / 2.0;
        double centroY = alto / 2.0;

        gc.setFill(Color.web("#07111f"));
        gc.fillRect(0, 0, ancho, alto);

        List<PuntoTelemetria> puntos = resultado.puntos();

        double maximo = 1.0;

        for (PuntoTelemetria punto : puntos) {
            maximo = Math.max(
                    maximo,
                    Math.hypot(punto.xKm(), punto.yKm())
            );
        }

        double escala = Math.min(ancho, alto) * 0.42 / maximo;

        gc.setStroke(Color.web("#2f89fc"));
        gc.setLineWidth(1.7);
        gc.beginPath();

        for (int i = 0; i <= indiceActual; i++) {

            PuntoTelemetria punto = puntos.get(i);

            double x = centroX + punto.xKm() * escala;
            double y = centroY - punto.yKm() * escala;

            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }

        gc.stroke();

        double radioTierraGrafico = Math.max(16, 6378.0 * escala);

        gc.setFill(Color.DODGERBLUE);
        gc.fillOval(
                centroX - radioTierraGrafico,
                centroY - radioTierraGrafico,
                radioTierraGrafico * 2,
                radioTierraGrafico * 2
        );

        gc.setFill(Color.WHITE);
        gc.fillText("TIERRA", centroX - 20, centroY + radioTierraGrafico + 18);

        PuntoTelemetria actual = puntos.get(indiceActual);

        double naveX = centroX + actual.xKm() * escala;
        double naveY = centroY - actual.yKm() * escala;

        gc.setFill(Color.ORANGE);
        gc.fillOval(naveX - 6, naveY - 6, 12, 12);
        gc.setFill(Color.WHITE);
        gc.fillText("Nave", naveX + 9, naveY - 8);

        double anguloLunar =
                2.0 * Math.PI
                * actual.tiempoSegundos()
                / (27.321661 * 86400.0);

        double radioOrbitaLunar =
                Math.min(ancho, alto) * 0.39;

        double lunaX =
                centroX + radioOrbitaLunar * Math.cos(anguloLunar);

        double lunaY =
                centroY - radioOrbitaLunar * Math.sin(anguloLunar);

        gc.setFill(Color.LIGHTGRAY);
        gc.fillOval(lunaX - 10, lunaY - 10, 20, 20);
        gc.setFill(Color.WHITE);
        gc.fillText("LUNA", lunaX + 12, lunaY);

        gc.setFill(Color.LIGHTGREEN);
        gc.fillText(
                "Punto "
                + (indiceActual + 1)
                + " de "
                + puntos.size(),
                20,
                alto - 20
        );

        resultado.reentrada().ifPresent(punto -> {
            gc.setFill(Color.RED);
            gc.fillText(
                    String.format(
                            "Reentrada detectada a %.2f h",
                            punto.tiempoSegundos() / 3600.0
                    ),
                    20,
                    25
            );
        });
    }

    private void dibujarSinSimulacion() {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#07111f"));
        gc.fillRect(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font(18));
        gc.fillText(
                "Configure los parámetros y pulse “Ejecutar simulación”.",
                190,
                290
        );
    }

    private void reiniciar() {

        resultado = null;
        indiceActual = 0;
        reproduciendo = false;
        ultimoInstante = 0;

        estado.setText("Estado: simulación reiniciada");
        tiempo.setText("Tiempo: --");
        altitud.setText("Altitud terrestre: --");
        distanciaLuna.setText("Distancia lunar: --");
        velocidad.setText("Velocidad: --");
        periapsis.setText("Periapsis lunar: --");

        dibujarSinSimulacion();
    }

    private static Label crearEtiqueta(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.setTextFill(Color.LIGHTGRAY);
        return etiqueta;
    }

    private static Label crearTituloSeccion(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.setTextFill(Color.LIGHTGREEN);
        etiqueta.setFont(Font.font(16));
        return etiqueta;
    }

    private static void configurarCampo(TextField campo) {
        campo.setMaxWidth(240);
    }

    /**
     * Punto de entrada de la interfaz.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}
