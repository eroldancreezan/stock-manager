package com.chebot.stock_manager.controller;

import com.chebot.stock_manager.model.Producto;
import com.chebot.stock_manager.service.ProductoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import org.springframework.stereotype.Controller; // Usar el @Controller de Spring
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;

@Controller
public class StockController {

    // Inyección del Servicio (Spring se encarga de crear esta instancia)
    private final ProductoService productoService;
    private final ObservableList<Producto> productosList = FXCollections.observableArrayList();

    // Constructor que Spring usa para inyectar ProductoService
    public StockController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // --- Componentes FXML ---
    // Campos de entrada para AGREGAR/EDITAR
    @FXML private TextField txtReferencia;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextArea txtObservaciones;
    @FXML private ImageView imgQrCode; // Necesitas añadir este fx:id en tu FXML
    // Tabla y sus componentes
    @FXML private TableView<Producto> tblStock;

    // Componentes para FILTRADO
    @FXML private TextField txtFiltroValor;
    @FXML private ComboBox<String> cmbFiltroCriterio;

    // --- Métodos de Inicialización ---

    @FXML
    public void initialize() {
        // Se ejecuta cuando el FXML es cargado, ANTES de @PostConstruct

        // Inicializar ComboBox de Estado (para agregar/editar)
        cmbEstado.getItems().addAll("Disponible", "Roto", "Desaparecido");
        cmbEstado.setValue("Disponible");

        // Inicializar ComboBox de Filtro
        cmbFiltroCriterio.getItems().addAll("Referencia", "Descripción", "Estado");
        cmbFiltroCriterio.setValue("Referencia");

        // 1. Configurar las columnas de la tabla (CRUCIAL para ver datos)
        configurarColumnasTabla();

        // 2. Enlaza la lista observable a la tabla
        tblStock.setItems(productosList);
        // Muestra el QR al hacer clic en un elemento de la tabla
        tblStock.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                mostrarQrEnVista(newSelection);
            }
        });
    }

    @PostConstruct // Se ejecuta después de que el Bean de Spring ha sido inicializado
    private void cargarDatosIniciales() {
        // 3. Carga los datos iniciales de la DB usando el servicio de Spring
        refrescarTabla();
    }

    // --- Configuración de Tabla ---

    private void configurarColumnasTabla() {
        // Se mapea cada columna de la tabla al nombre de la propiedad en Producto.java

        TableColumn<Producto, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Producto, String> refCol = new TableColumn<>("Referencia");
        refCol.setCellValueFactory(new PropertyValueFactory<>("referencia"));

        TableColumn<Producto, String> descCol = new TableColumn<>("Descripción");
        descCol.setCellValueFactory(new PropertyValueFactory<>("descripcion"));

        TableColumn<Producto, Integer> cantCol = new TableColumn<>("Cantidad");
        cantCol.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        TableColumn<Producto, String> estadoCol = new TableColumn<>("Estado");
        estadoCol.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<Producto, String> obsCol = new TableColumn<>("Observaciones");
        obsCol.setCellValueFactory(new PropertyValueFactory<>("observaciones"));

        // Limpia columnas existentes y añade las nuevas
        tblStock.getColumns().clear();
        tblStock.getColumns().addAll(idCol, refCol, descCol, cantCol, estadoCol, obsCol);
    }


    // --- Métodos de Acción (Llamados por FXML) ---

    @FXML
    public void agregarProducto() {
        try {
            Producto nuevoProducto = new Producto(
                    txtReferencia.getText(),
                    txtDescripcion.getText(),
                    // Se utiliza Integer.parseInt para convertir el texto de cantidad
                    Integer.parseInt(txtCantidad.getText()),
                    cmbEstado.getValue(),
                    txtObservaciones.getText()
            );

            productoService.guardarProducto(nuevoProducto);
            refrescarTabla();
            limpiarCampos();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto agregado correctamente.");

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "La Cantidad debe ser un número válido.");
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Validación", e.getMessage());
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de DB", "Ocurrió un error al guardar: " + e.getMessage());
        }
    }

    @FXML
    public void filtrarProductos() {
        String criterio = cmbFiltroCriterio.getValue();
        String valor = txtFiltroValor.getText();

        List<Producto> productosFiltrados = productoService.filtrarProductos(criterio, valor);

        productosList.clear();
        productosList.addAll(productosFiltrados);
    }

    @FXML
    public void refrescarTabla() {
        productosList.clear();
        productosList.addAll(productoService.obtenerTodosLosProductos());
    }

    // --- Métodos Auxiliares ---

    private void limpiarCampos() {
        txtReferencia.clear();
        txtDescripcion.clear();
        txtCantidad.clear();
        txtObservaciones.clear();
        cmbEstado.setValue("Disponible");
    }

    private void mostrarAlerta(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void mostrarQrEnVista(Producto producto) {
        byte[] qrBytes = producto.getCodigoQr();
        if (qrBytes != null) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(qrBytes)) {
                javafx.scene.image.Image qrImage = new javafx.scene.image.Image(bis);
                imgQrCode.setImage(qrImage);
            } catch (IOException e) {
                e.printStackTrace();
                imgQrCode.setImage(null);
            }
        } else {
            imgQrCode.setImage(null);
        }
    }
    @FXML
    public void exportarQrSeleccionado() {

        // Obtener el producto actualmente seleccionado en la tabla
        Producto seleccionado = tblStock.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Advertencia", "Por favor, selecciona un producto de la tabla.");
            return;
        }

        // 1. Abrir el diálogo para elegir la carpeta de destino
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Seleccionar Carpeta para Guardar QR");
        File rutaDestino = dirChooser.showDialog(tblStock.getScene().getWindow()); // Usa la ventana actual

        if (rutaDestino != null) {
            try {
                // 2. Llamar al servicio para guardar el archivo
                String rutaGuardada = productoService.exportarQrCode(seleccionado.getId(), rutaDestino);

                mostrarAlerta(Alert.AlertType.INFORMATION,
                        "Exportación Exitosa",
                        "Código QR guardado en:\n" + rutaGuardada);

            } catch (IOException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de Archivo",
                        "No se pudo guardar el archivo QR: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de Producto", e.getMessage());
            }
        }
    }

}