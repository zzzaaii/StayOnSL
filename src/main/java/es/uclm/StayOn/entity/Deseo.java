package es.uclm.StayOn.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "deseo",
    uniqueConstraints = @UniqueConstraint(columnNames = {"inquilino_id", "inmueble_id"})
)
public class Deseo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inquilino_id", nullable = false)
    private Inquilino inquilino;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inmueble_id", nullable = false)
    private Inmueble inmueble;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Inquilino getInquilino() { return inquilino; }
    public void setInquilino(Inquilino inquilino) { this.inquilino = inquilino; }

    public Inmueble getInmueble() { return inmueble; }
    public void setInmueble(Inmueble inmueble) { this.inmueble = inmueble; }
}
