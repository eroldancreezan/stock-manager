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
import java.util.Optional;

@Controller
public class StockController {

    // Campo para almacenar temporalmente el ID del producto que se está editando/actualizando.
    private Long productoEnEdicionId = null;
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
    @FXML private Spinner<Integer> spinnerCantidad;
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
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10000, 1);
        spinnerCantidad.setValueFactory(valueFactory);
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
        // Opcional: Configurar la tabla para que con doble clic cargue los datos
        tblStock.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                cargarProductoParaEdicion();
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
            // 1. Creamos el objeto con los datos del formulario
            Producto productoAguardar = new Producto(
                    txtReferencia.getText(),
                    txtDescripcion.getText(),
                    spinnerCantidad.getValue(),
                    cmbEstado.getValue(),
                    txtObservaciones.getText()
            );

            // --- SOLUCIÓN DEL ERROR UNIQUE ---
            // 2. Si la variable tiene un ID, se lo asignamos al producto.
            // Esto le dice al Servicio: "¡Es una edición, no una inserción!"
            if (productoEnEdicionId != null) {
                productoAguardar.setId(productoEnEdicionId);
            }

            // 3. Llamamos al servicio
            // Si tiene ID -> El servicio usará productoRepository.save() (UPDATE) -> ¡ÉXITO!
            // Si no tiene ID -> El servicio usará JDBC (INSERT) -> ¡ÉXITO!
            productoService.guardarProducto(productoAguardar);

            // 4. Limpieza
            refrescarTabla();
            limpiarCampos(); // Esto debe poner productoEnEdicionId = null

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Operación realizada correctamente.");

        } catch (NumberFormatException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "La cantidad debe ser un número.");
        } catch (Exception e) {
            // Aquí verás el mensaje de error si algo más falla
            e.printStackTrace(); // Imprime en consola para ver detalles
            mostrarAlerta(Alert.AlertType.ERROR, "Error de DB", e.getMessage());
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
        spinnerCantidad.getValueFactory().setValue(1);
        txtObservaciones.clear();
        cmbEstado.setValue("Disponible");

        // IMPORTANTE: Reiniciar el ID de edición a null
        productoEnEdicionId = null;
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
    @FXML
    public void cargarProductoParaEdicion() {
        Producto seleccionado = tblStock.getSelectionModel().getSelectedItem();

        if (seleccionado != null) {
            // --- AQUÍ ESTÁ LA CLAVE ---
            // Guardamos el ID en nuestra variable temporal
            productoEnEdicionId = seleccionado.getId();

            // Cargar los datos visuales
            txtReferencia.setText(seleccionado.getReferencia());
            txtDescripcion.setText(seleccionado.getDescripcion());
            spinnerCantidad.getValueFactory().setValue(seleccionado.getCantidad());
            cmbEstado.setValue(seleccionado.getEstado());
            txtObservaciones.setText(seleccionado.getObservaciones());

            // (Opcional) Mostrar el QR si lo tienes implementado
        }
    }
    @FXML
    public void eliminarProducto() {
        Producto seleccionado = tblStock.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Advertencia", "Por favor, selecciona un producto para eliminar.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "¿Estás seguro de que quieres eliminar la referencia: " + seleccionado.getReferencia() + "?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                productoService.eliminarProducto(seleccionado.getId());
                refrescarTabla();
                limpiarCampos();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Producto eliminado correctamente.");
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de DB", "No se pudo eliminar el producto.");
            }
        }
    }


}