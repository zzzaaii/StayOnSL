package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.Date;


@Entity
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodo;

    // Referencia única del pago
    private String referencia;

    @OneToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    // NO SE GUARDAN EN LA BASE DE DATOS
    @Transient
    private String numeroTarjeta;
    @Transient
    private String fechaCaducidad; // Formato "YYYY-MM"
    @Transient
    private String cvv;
    @Transient
    private String emailPaypal;

    //  información de reembolso
    private boolean reembolsado = false;

    private Double importeReembolsado;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaReembolso;

    // GETTERS / SETTERS 

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MetodoPago getMetodo() { return metodo; }
    public void setMetodo(MetodoPago metodo) { this.metodo = metodo; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public String getNumeroTarjeta() { return numeroTarjeta; }
    public void setNumeroTarjeta(String numeroTarjeta) { this.numeroTarjeta = numeroTarjeta; }

    public String getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(String fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getEmailPaypal() { return emailPaypal; }
    public void setEmailPaypal(String emailPaypal) { this.emailPaypal = emailPaypal; }

    public boolean isReembolsado() { return reembolsado; }
    public void setReembolsado(boolean reembolsado) { this.reembolsado = reembolsado; }

    public Double getImporteReembolsado() { return importeReembolsado; }
    public void setImporteReembolsado(Double importeReembolsado) { this.importeReembolsado = importeReembolsado; }

    public Date getFechaReembolso() { return fechaReembolso; }
    public void setFechaReembolso(Date fechaReembolso) { this.fechaReembolso = fechaReembolso; }
}
