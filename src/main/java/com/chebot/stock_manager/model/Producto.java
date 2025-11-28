package com.chebot.stock_manager.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
// import org.hibernate.annotations.Type; // ESTO YA NO ES NECESARIO Y DEBE ELIMINARSE

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    // CRUCIAL: Se usa IDENTITY para indicar a JPA que la DB genera el ID.
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Mantenemos columnDefinition="INTEGER" como medida de seguridad para SQLite.
    @Column(columnDefinition = "INTEGER")
    private Long id;

    @Column(nullable = false, unique = true)
    private String referencia;

    @Column(nullable = false)
    private String descripcion;

    private Integer cantidad;

    private String estado;

    @Lob
    private String observaciones;

    @JdbcTypeCode(Types.VARBINARY)
    private byte[] codigoQr;

    // --- Constructores ---
    public Producto() {
    }

    public Producto(String referencia, String descripcion, Integer cantidad, String estado, String observaciones) {
        this.referencia = referencia;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    // --- Getters y Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public byte[] getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(byte[] codigoQr) {
        this.codigoQr = codigoQr;
    }
}