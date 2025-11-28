package com.chebot.stock_manager.fx;

import com.chebot.stock_manager.StockManagerApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

public class StockManagerFXApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        // Inicializa el contexto de Spring Boot ANTES de iniciar la GUI
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = SpringApplication.run(StockManagerApplication.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/stock_view.fxml"));

        // ¡LA CLAVE DE LA INTEGRACIÓN!
        // Le dice a JavaFX que use el contexto de Spring para crear el controlador
        loader.setControllerFactory(applicationContext::getBean);

        Parent root = loader.load();

        primaryStage.setTitle("Chebot - Gestión de Stock");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Cierra el contexto de Spring al cerrar la ventana
        this.applicationContext.close();
        // Cierra la aplicación JavaFX
        javafx.application.Platform.exit();
    }
}