package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaInicio;

    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private Date fechaFin;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean pagado = false;

    @ManyToOne
    private Inquilino inquilino;

    @ManyToOne
    private Inmueble inmueble;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Date getFechaFin() { return fechaFin; }
    public void setFechaFin(Date fechaFin) { this.fechaFin = fechaFin; }

    public boolean isPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }

    public Inquilino getInquilino() { return inquilino; }
    public void setInquilino(Inquilino inquilino) { this.inquilino = inquilino; }

    public Inmueble getInmueble() { return inmueble; }
    public void setInmueble(Inmueble inmueble) { this.inmueble = inmueble; }

    // Método auxiliar: comprueba si la reserva está activa hoy
    public boolean isActiva() {
        Date hoy = new Date();
        return hoy.compareTo(fechaInicio) >= 0 && hoy.compareTo(fechaFin) <= 0;
    }
}
