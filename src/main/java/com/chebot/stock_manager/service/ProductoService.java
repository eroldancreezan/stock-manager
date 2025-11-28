package com.chebot.stock_manager.service;

import com.chebot.stock_manager.model.Producto;
import com.chebot.stock_manager.repository.ProductoRepository;
import com.google.zxing.WriterException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.FileOutputStream; // Nuevo import
import java.io.File;             // Nuevo import

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProductoService(ProductoRepository productoRepository, JdbcTemplate jdbcTemplate) {
        this.productoRepository = productoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    // --- Métodos Auxiliares ---
    private String generarContenidoQr(Producto producto) {
        return "Referencia:" + producto.getReferencia() + "\nDescripción: "
                + producto.getDescripcion() + "\nCantidad: " + producto.getCantidad()
                + "\nEstado: " + producto.getEstado();
    }

    // --- CRUD Y LÓGICA DE NEGOCIO ---

    @Transactional
    public Producto guardarProducto(Producto producto) {

        // 1. VALIDACIONES
        if (producto.getCantidad() == null || producto.getCantidad() < 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número positivo.");
        }

        // 2. GENERACIÓN DE QR
        String qrContent = generarContenidoQr(producto);
        try {
            byte[] qrImageBytes = QrCodeGenerator.generateQrCodeImage(qrContent);
            producto.setCodigoQr(qrImageBytes);
        } catch (WriterException | IOException e) {
            System.err.println("Advertencia: Error al generar el Código QR. Se insertará sin QR.");
            producto.setCodigoQr(null);
        }

        // 3. MANEJO DE INSERCIÓN/EDICIÓN

        // A) SI ES UNA EDICIÓN: Usa el método estándar de JPA.
        if (producto.getId() != null) {
            return productoRepository.save(producto);
        }

        // B) SI ES UN PRODUCTO NUEVO: Usa JDBC Puro y Nativo (SOLUCIÓN FINAL)

        String insertSql = "INSERT INTO productos (referencia, descripcion, cantidad, estado, observaciones, codigo_qr) VALUES (?, ?, ?, ?, ?, ?)";
        String selectIdSql = "SELECT last_insert_rowid()"; // Función nativa de SQLite

        try {
            // 1. Ejecutar la inserción (sin KeyHolder que falla)
            jdbcTemplate.update(insertSql,
                    producto.getReferencia(),
                    producto.getDescripcion(),
                    producto.getCantidad(),
                    producto.getEstado(),
                    producto.getObservaciones(),
                    producto.getCodigoQr()
            );

            // 2. RECUPERACIÓN NATIVA: Usamos un RowMapper para leer explícitamente el primer valor del ResultSet (columna 1).
            Long generatedId = jdbcTemplate.queryForObject(selectIdSql, (rs, rowNum) -> rs.getLong(1));

            // 3. Buscar el objeto por el ID para tenerlo en la sesión de Hibernate (JPA)
            return productoRepository.findById(generatedId)
                    .orElseThrow(() -> new RuntimeException("Error: No se pudo cargar el producto después de la inserción."));

        } catch (Exception e) {
            throw new RuntimeException("Error al insertar producto vía JDBC: " + e.getMessage(), e);
        }
    }

    // --- MÉTODOS DE LECTURA Y FILTRADO ---

    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> filtrarProductos(String criterio, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return obtenerTodosLosProductos();
        }

        return switch (criterio.toLowerCase()) {
            case "referencia" -> productoRepository.findByReferenciaContainingIgnoreCase(valor);
            case "descripcion" -> productoRepository.findByDescripcionContainingIgnoreCase(valor);
            case "estado" -> productoRepository.findByEstado(valor);
            default -> obtenerTodosLosProductos();
        };
    }
    /**
     * Exporta el array de bytes del código QR a un archivo PNG en la ruta especificada.
     * @return La ruta absoluta del archivo guardado.
     */
    @Transactional(readOnly = true) // Solo lee datos de la DB
    public String exportarQrCode(Long productoId, File rutaDestino) throws IOException {

        // 1. Obtener el producto de la DB
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        byte[] qrBytes = producto.getCodigoQr();

        if (qrBytes == null) {
            throw new IOException("El producto no tiene un Código QR generado.");
        }

        // 2. Definir el nombre del archivo
        String nombreArchivo = "QR_" + producto.getReferencia() + ".png";
        File archivo = new File(rutaDestino, nombreArchivo);

        // 3. Escribir los bytes al disco
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(qrBytes);
        }

        return archivo.getAbsolutePath();
    }
}