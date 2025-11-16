package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Reserva {

    // 🆕 ENUM interno
    public enum EstadoReserva {
        PENDIENTE,    // solicitud enviada, esperando decisión
        ACEPTADA,     // aceptada por el propietario (falta pagar)
        RECHAZADA,    // rechazada por el propietario
        CONFIRMADA    // pagada y cerrada
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private Date fechaInicio;

    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private Date fechaFin;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean pagado = false;

    // 🆕 Estado de la reserva
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @ManyToOne
    private Inquilino inquilino;

    @ManyToOne
    private Inmueble inmueble;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    // =======================
    // GETTERS Y SETTERS
    // =======================

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

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    // 🆕 getters y setters del estado
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    // =======================
    // MÉTODOS AUXILIARES
    // =======================

    public boolean isActiva() {
        Date hoy = new Date();
        return hoy.compareTo(fechaInicio) >= 0 && hoy.compareTo(fechaFin) <= 0;
    }

    @Transient
    public Double getPrecioTotal() {
        if (fechaInicio == null || fechaFin == null || inmueble == null || inmueble.getPrecioPorNoche() == null) {
            return null;
        }

        java.time.LocalDate inicio;
        java.time.LocalDate fin;

        if (fechaInicio instanceof java.sql.Date && fechaFin instanceof java.sql.Date) {
            inicio = ((java.sql.Date) fechaInicio).toLocalDate();
            fin = ((java.sql.Date) fechaFin).toLocalDate();
        } else {
            inicio = fechaInicio.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            fin = fechaFin.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }

        long noches = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin);
        if (noches < 1) noches = 1;

        return inmueble.getPrecioPorNoche() * noches;
    }

    public String getDireccion() {
        return (inmueble != null) ? inmueble.getDireccion() : null;
    }
}
