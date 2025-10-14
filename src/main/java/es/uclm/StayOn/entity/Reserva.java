package es.uclm.StayOn.entity;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;


@Entity
public class Reserva {

	@Column
	Pago pago;
	@Column
	SolicitudReserva solicitud;
	@Column
	Inquilino inquilino;
	@Column
	Inmueble inmueble;
	@Column
	PoliticaCancelacion politicaCancelacion;
	@Column
	private Date fechaInicio;
	@Column
	private Date fechaFin;

	public Pago getPago() {
		return pago;
	}

	public void setPago(Pago pago) {
		this.pago = pago;
	}

	public SolicitudReserva getSolicitud() {
		return solicitud;
	}

	public void setSolicitud(SolicitudReserva solicitud) {
		this.solicitud = solicitud;
	}

	public Inquilino getInquilino() {
		return inquilino;
	}

	public void setInquilino(Inquilino inquilino) {
		this.inquilino = inquilino;
	}

	public Inmueble getInmueble() {
		return inmueble;
	}

	public void setInmueble(Inmueble inmueble) {
		this.inmueble = inmueble;
	}

	public PoliticaCancelacion getPoliticaCancelacion() {
		return politicaCancelacion;
	}

	public void setPoliticaCancelacion(PoliticaCancelacion politicaCancelacion) {
		this.politicaCancelacion = politicaCancelacion;
	}

	public Date getFechaInicio() {
		return fechaInicio;
	}

	public void setFechaInicio(Date fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	public Date getFechaFin() {
		return fechaFin;
	}

	public void setFechaFin(Date fechaFin) {
		this.fechaFin = fechaFin;
	}

	public void isPagado() {
		// TODO - implement Reserva.isPagado
		throw new UnsupportedOperationException();
	}

	public void isActiva() {
		// TODO - implement Reserva.isActiva
		throw new UnsupportedOperationException();
	}
}
