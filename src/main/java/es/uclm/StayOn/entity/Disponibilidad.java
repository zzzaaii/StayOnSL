package es.uclm.StayOn.entity;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Disponibilidad {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Inmueble inmueble;
	@Column
	PoliticaCancelacion politicaCancelación;
	@Column
	private Date fechaInicio;
	@Column
	private Date fechaFin;
	@Column
	private double precio;
	@Column
	private boolean directa;
	public Inmueble getInmueble() {
		return inmueble;
	}
	public void setInmueble(Inmueble inmueble) {
		this.inmueble = inmueble;
	}
	public PoliticaCancelacion getPoliticaCancelación() {
		return politicaCancelación;
	}
	public void setPoliticaCancelación(PoliticaCancelacion politicaCancelación) {
		this.politicaCancelación = politicaCancelación;
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
	public double getPrecio() {
		return precio;
	}
	public void setPrecio(double precio) {
		this.precio = precio;
	}
	public boolean isDirecta() {
		return directa;
	}
	public void setDirecta(boolean directa) {
		this.directa = directa;
	}
}
