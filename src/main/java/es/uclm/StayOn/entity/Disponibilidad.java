package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaInicio;

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaFin;

    // ⚠️ Este precio no se mostrará al propietario, solo se usará internamente
    private Double precio;

    private boolean directa; // Reserva inmediata

    @ManyToOne
    @JoinColumn(name = "inmueble_id")
    private Inmueble inmueble;

    public Disponibilidad() {}

    // ======= Getters y Setters =======
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Date getFechaFin() { return fechaFin; }
    public void setFechaFin(Date fechaFin) { this.fechaFin = fechaFin; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public boolean isDirecta() { return directa; }
    public void setDirecta(boolean directa) { this.directa = directa; }

    public Inmueble getInmueble() { return inmueble; }
    public void setInmueble(Inmueble inmueble) { this.inmueble = inmueble; }

    // ======= Nuevo método auxiliar =======
    @Transient
    public Double getPrecioTotal() {
        if (fechaInicio == null || fechaFin == null || inmueble == null) {
            return null;
        }

        // Calculamos número de noches entre las fechas
        long noches = ChronoUnit.DAYS.between(
            fechaInicio.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
            fechaFin.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );

        // Evita valores negativos
        if (noches < 1) noches = 1;

        // Multiplicamos por el precio por noche del inmueble
        return inmueble.getPrecioPorNoche() * noches;
    }
}

