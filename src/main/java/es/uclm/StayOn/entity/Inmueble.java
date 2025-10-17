package es.uclm.StayOn.entity;

import jakarta.persistence.*;

@Entity
public class Inmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String tipo;
    @Column
    private String descripcion;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private double precioNoche = 0.0; 

    @ManyToOne
    @JoinColumn(name = "propietario_id", nullable = false)
    private Propietario propietario;

    // Getters y Setters
    public Long getId() { 
        return id; 
    }

    public void setId(Long id) { 
        this.id = id; 
    }

    public String getDireccion() { 
        return direccion; 
    }

    public void setDireccion(String direccion) { 
        this.direccion = direccion; 
    }

    public double getPrecioPorNoche() { 
        return precioNoche; 
    }

    public void setPrecioPorNoche(double precioPorNoche) { 
        this.precioNoche = precioPorNoche; 
    }

    public Propietario getPropietario() { 
        return propietario; 
    }

    public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public double getPrecioNoche() {
		return precioNoche;
	}

	public void setPrecioNoche(double precioNoche) {
		this.precioNoche = precioNoche;
	}

	public void setPropietario(Propietario propietario) { 
        this.propietario = propietario; 
    }
}
