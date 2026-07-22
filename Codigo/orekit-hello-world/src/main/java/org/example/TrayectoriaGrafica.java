package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * Interfaz gráfica de la Misión Oasis Lunar.
 * Representa visualmente la Tierra, la Luna, la nave espacial
 * y el recorrido de una trayectoria lunar mediante JavaFX.
 */
public class TrayectoriaGrafica extends Application {

    private double angulo = 0;

    /**
     * Construye y muestra la escena principal del simulador.
     *
     * @param stage ventana principal proporcionada por JavaFX
     */
    @Override
    public void start(Stage stage) {

        Canvas canvas = new Canvas(900, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Label titulo = new Label("MISIÓN OASIS LUNAR");
        titulo.setFont(Font.font(24));
        titulo.setTextFill(Color.WHITE);

        Label subtitulo = new Label("Prueba de concepto visual de trayectoria orbital");
        subtitulo.setTextFill(Color.LIGHTGRAY);

        VBox encabezado = new VBox(5, titulo, subtitulo);
        encabezado.setPadding(new Insets(15));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #07111f;");
        root.setTop(encabezado);
        root.setCenter(canvas);

        new AnimationTimer() {
            /**
             * Actualiza la animación de la nave en cada cuadro.
             *
             * @param now tiempo actual de la animación en nanosegundos
             */
            @Override
            public void handle(long now) {
                dibujar(gc, canvas.getWidth(), canvas.getHeight());
                angulo += 0.01;
            }
        }.start();

        Scene scene = new Scene(root, 900, 680);

        stage.setTitle("Misión Oasis Lunar - JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    private void dibujar(GraphicsContext gc, double ancho, double alto) {

        gc.setFill(Color.web("#07111f"));
        gc.fillRect(0, 0, ancho, alto);

        double centroX = ancho / 2;
        double centroY = alto / 2;

        gc.setStroke(Color.web("#2f89fc"));
        gc.setLineWidth(2);

        gc.beginPath();

        for (double t = 0; t <= Math.PI * 2; t += 0.01) {
            double x = centroX + 260 * Math.sin(t);
            double y = centroY + 150 * Math.sin(t) * Math.cos(t);

            if (t == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }

        gc.stroke();

        gc.setFill(Color.DODGERBLUE);
        gc.fillOval(centroX - 38, centroY - 38, 76, 76);

        gc.setFill(Color.WHITE);
        gc.fillText("TIERRA", centroX - 22, centroY + 58);

        double lunaX = centroX + 260;
        double lunaY = centroY;

        gc.setFill(Color.LIGHTGRAY);
        gc.fillOval(lunaX - 23, lunaY - 23, 46, 46);
        gc.setFill(Color.WHITE);
        gc.fillText("LUNA", lunaX - 18, lunaY + 42);

        double naveX = centroX + 260 * Math.sin(angulo);
        double naveY = centroY + 150 * Math.sin(angulo) * Math.cos(angulo);

        gc.setFill(Color.ORANGE);
        gc.fillOval(naveX - 8, naveY - 8, 16, 16);

        gc.setFill(Color.WHITE);
        gc.fillText("Nave", naveX + 12, naveY - 10);

        gc.setFill(Color.LIGHTGREEN);
        gc.fillText("Estado: simulación activa", 25, alto - 40);
        gc.fillText("Motor gráfico: JavaFX AnimationTimer", 25, alto - 20);
    }

    /**
     * Punto de entrada de la interfaz gráfica.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}
