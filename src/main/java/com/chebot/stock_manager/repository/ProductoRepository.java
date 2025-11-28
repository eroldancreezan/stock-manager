package com.chebot.stock_manager.repository;

import com.chebot.stock_manager.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// JpaRepository toma la Entidad (Producto) y el tipo de su ID (Long)
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // --- Métodos de Filtrado Personalizado ---
    Optional<Producto> findByReferencia(String referencia);
    // 1. Filtrar por Referencia (Spring Data JPA infiere el query por el nombre del método)
    List<Producto> findByReferenciaContainingIgnoreCase(String referencia);

    // 2. Filtrar por Descripción
    List<Producto> findByDescripcionContainingIgnoreCase(String descripcion);

    // 3. Filtrar por Estado (búsqueda exacta)
    List<Producto> findByEstado(String estado);

}