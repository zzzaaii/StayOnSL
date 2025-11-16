package es.uclm.StayOn.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@DiscriminatorValue("PROPIETARIO")
public class Propietario extends Usuario {

    @OneToMany(mappedBy = "propietario")
    private List<Inmueble> inmuebles;

    public List<Inmueble> getInmuebles() { return inmuebles; }
    public void setInmuebles(List<Inmueble> inmuebles) { this.inmuebles = inmuebles; }
}
