package com.chebot.stock_manager;

import com.chebot.stock_manager.fx.StockManagerFXApplication; // Importación corregida
import javafx.application.Application; // Necesario para llamar a launch
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class StockManagerApplication {

	public static void main(String[] args) {
		// Lanzamos la aplicación JavaFX, que a su vez se encarga de iniciar Spring
		Application.launch(StockManagerFXApplication.class, args);
	}
}