package es.uclm.StayOn.entity;
import jakarta.persistence.*;
import java.util.UUID;

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

  //NO SE GUARDAN EN LA BASE DE DATOS
    @Transient
    private String numeroTarjeta;
    @Transient
    private String fechaCaducidad; // Formato "YYYY-MM"
    @Transient
    private String cvv;
    @Transient
    private String emailPaypal;


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
}

