package es.uclm.StayOn.entity;

import jakarta.persistence.*;

@Entity
public class Inmueble {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private double precioPorNoche = 0.0; 

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
        return precioPorNoche; 
    }

    public void setPrecioPorNoche(double precioPorNoche) { 
        this.precioPorNoche = precioPorNoche; 
    }

    public Propietario getPropietario() { 
        return propietario; 
    }

    public void setPropietario(Propietario propietario) { 
        this.propietario = propietario; 
    }
}
