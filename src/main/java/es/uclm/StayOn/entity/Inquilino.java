package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("INQUILINO")
public class Inquilino extends Usuario {

    @OneToMany(mappedBy = "inquilino")
    private List<Reserva> reservas;

    public List<Reserva> getReservas() { return reservas; }
    public void setReservas(List<Reserva> reservas) { this.reservas = reservas; }
}
