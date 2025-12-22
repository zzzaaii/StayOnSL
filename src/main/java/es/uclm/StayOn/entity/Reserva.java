package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class Reserva {

    public enum EstadoReserva {
        PENDIENTE,
        ACEPTADA,
        RECHAZADA,
        CONFIRMADA
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
    private boolean pagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE;

    @ManyToOne
    private Inquilino inquilino;

    @ManyToOne
    private Inmueble inmueble;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    @Column(nullable = false)
    private Double precioPorNocheAplicado = 0.0;

    @Enumerated(EnumType.STRING)
    private PoliticaCancelacion politicaCancelacionAplicada;

    @Column(nullable = false)
    private boolean reservaDirectaAplicada;

    @Column(nullable = false)
    private Double precioTotal = 0.0;

    // ✅ NUEVO: ocultar reserva solo en la lista del inquilino (sin borrar en BD)
    @Column(nullable = false)
    private boolean ocultaParaInquilino = false;

    // ---------------- Getters / Setters ----------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Date getFechaFin() { return fechaFin; }
    public void setFechaFin(Date fechaFin) { this.fechaFin = fechaFin; }

    public boolean isPagado() { return pagado; }
    public void setPagado(boolean pagado) { this.pagado = pagado; }

    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva estado) { this.estado = estado; }

    public Inquilino getInquilino() { return inquilino; }
    public void setInquilino(Inquilino inquilino) { this.inquilino = inquilino; }

    public Inmueble getInmueble() { return inmueble; }
    public void setInmueble(Inmueble inmueble) { this.inmueble = inmueble; }

    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }

    public Double getPrecioPorNocheAplicado() { return precioPorNocheAplicado; }
    public void setPrecioPorNocheAplicado(Double precioPorNocheAplicado) {
        this.precioPorNocheAplicado = (precioPorNocheAplicado != null) ? precioPorNocheAplicado : 0.0;
    }

    public PoliticaCancelacion getPoliticaCancelacionAplicada() {
        return politicaCancelacionAplicada;
    }

    public void setPoliticaCancelacionAplicada(PoliticaCancelacion politicaCancelacionAplicada) {
        this.politicaCancelacionAplicada = politicaCancelacionAplicada;
    }

    public boolean getReservaDirectaAplicada() {
        return reservaDirectaAplicada;
    }

    public boolean isReservaDirectaAplicada() {
        return getReservaDirectaAplicada();
    }

    public void setReservaDirectaAplicada(boolean reservaDirectaAplicada) {
        this.reservaDirectaAplicada = reservaDirectaAplicada;
    }

    public Double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(Double precioTotal) {
        this.precioTotal = (precioTotal != null) ? precioTotal : 0.0;
    }

    // ✅ NUEVO getters/setters
    public boolean isOcultaParaInquilino() { return ocultaParaInquilino; }
    public void setOcultaParaInquilino(boolean ocultaParaInquilino) { this.ocultaParaInquilino = ocultaParaInquilino; }

    public boolean isActiva() {
        Date hoy = new Date();
        return hoy.compareTo(fechaInicio) >= 0 && hoy.compareTo(fechaFin) <= 0;
    }

    public String getDireccion() {
        return (inmueble != null) ? inmueble.getDireccion() : null;
    }

    @Transient
    public long getNoches() {
        if (fechaInicio == null || fechaFin == null) {
            return 0;
        }

        java.time.LocalDate inicio;
        java.time.LocalDate fin;

        if (fechaInicio instanceof java.sql.Date sqlInicio && fechaFin instanceof java.sql.Date sqlFin) {
            inicio = sqlInicio.toLocalDate();
            fin = sqlFin.toLocalDate();
        } else {
            inicio = fechaInicio.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            fin = fechaFin.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        long noches = java.time.temporal.ChronoUnit.DAYS.between(inicio, fin);
        return (noches < 1) ? 1 : noches;
    }
}
