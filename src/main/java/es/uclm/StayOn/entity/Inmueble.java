package es.uclm.StayOn.entity;

import jakarta.persistence.*;

@Entity
public class Inmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tipo;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    @Column(nullable = false)
    private Double precioPorNoche = 0.0; // valor por defecto para evitar null

    @ManyToOne
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Disponibilidad disponibilidad;
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
 // Getters y setters correctos
    public Disponibilidad getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(Disponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public Double getPrecioPorNoche() { return precioPorNoche; }
    public void setPrecioPorNoche(Double precioPorNoche) { this.precioPorNoche = precioPorNoche; }

    public Propietario getPropietario() { return propietario; }
    public void setPropietario(Propietario propietario) { this.propietario = propietario; }
}

