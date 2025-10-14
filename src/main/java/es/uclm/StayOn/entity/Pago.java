package es.uclm.StayOn.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Pago {

	@Column
	MetodoPago metodo;
	@Column
	Reserva reserva;
	@Column
	private UUID referencia;
}
