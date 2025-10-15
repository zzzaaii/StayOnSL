package es.uclm.StayOn.entity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INQUILINO")
public class Inquilino  extends Usuario {
	//coleccion de reservas 
	//atributos y relaciones específicas de inquilino
}
