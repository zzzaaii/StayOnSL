package es.uclm.StayOn.entity;

public class Inmueble {

	Propietario propietario;
	Collection<Reserva> reservas;
	Collection<ListaDeseos> listaDeseos;
	Collection<Disponibilidad> disponibilidades;
	Collection<SolicitudReserva> solicitudesReserva;
	private String direccion;
	private double precioNoche;
}
