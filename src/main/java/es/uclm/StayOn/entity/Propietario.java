package es.uclm.StayOn.entity;
import java.util.*;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PROPIETARIO")
public class Propietario  extends Usuario{

	//Collection<Inmueble> propiedades;
}
