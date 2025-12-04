package com.chebot.stock_manager.fx;

import com.chebot.stock_manager.StockManagerApplication;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URL;

public class StockManagerFXApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = SpringApplication.run(StockManagerApplication.class, args);
    }

    @Override
    public void start(Stage splashStage) throws IOException {
        // 1. Cargar el Splash Screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/splash_view.fxml"));
        Parent splashRoot = loader.load();

        Scene splashScene = new Scene(splashRoot);
        splashStage.setScene(splashScene);
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.show();

        // 2. Animación de carga
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(3), splashRoot);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);

        fadeIn.setOnFinished(e -> {
            try {
                mostrarVentanaPrincipal(); // Abrir la app real
                splashStage.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        fadeIn.play();
    }

    // --- AQUÍ ESTÁ LA CORRECCIÓN DE ESTILO ---
    private void mostrarVentanaPrincipal() throws IOException {
        Stage mainStage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/stock_view.fxml"));

        // Conectar con Spring Boot
        loader.setControllerFactory(applicationContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root);

        // --- CARGAR CSS DE FORMA SEGURA ---
        // Buscamos el archivo style.css en la raíz de resources
        URL cssUrl = getClass().getResource("/style.css");

        if (cssUrl == null) {
            System.err.println("❌ ERROR CRÍTICO: No se pudo encontrar el archivo 'style.css'. \n" +
                    "Asegúrate de que esté en 'src/main/resources/style.css'");
        } else {
            System.out.println("✅ CSS encontrado en: " + cssUrl.toExternalForm());
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        // ------------------------------------

        mainStage.setTitle("Chebot - Gestión de Stock");
        mainStage.setScene(scene);
        mainStage.show();
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        javafx.application.Platform.exit();
    }
}