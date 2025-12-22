package es.uclm.StayOn.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class DisponibilidadTest {

    private static Date utilDate(int y, int m, int d) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(y, m - 1, d, 0, 0, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    @DisplayName("getPrecioTotal -> null si falta fechaInicio o fechaFin o inmueble")
    void getPrecioTotal_nulls() {
        Disponibilidad disp = new Disponibilidad();

        
        assertThat(disp.getPrecioTotal()).isNull();

       
        disp.setFechaInicio(utilDate(2026, 1, 1));
        disp.setFechaFin(utilDate(2026, 1, 3));
        assertThat(disp.getPrecioTotal()).isNull();

        
        Inmueble inm = new Inmueble();
        inm.setPrecioPorNoche(50.0);
        disp.setInmueble(inm);
        disp.setFechaFin(null);
        assertThat(disp.getPrecioTotal()).isNull();
    }

    @Test
    @DisplayName("getPrecioTotal -> calcula noches y multiplica precioPorNoche")
    void getPrecioTotal_ok() {
        Inmueble inm = new Inmueble();
        inm.setPrecioPorNoche(40.0);

        Disponibilidad disp = new Disponibilidad();
        disp.setInmueble(inm);
        disp.setFechaInicio(utilDate(2026, 6, 1));
        disp.setFechaFin(utilDate(2026, 6, 4)); 

        Double total = disp.getPrecioTotal();
        assertThat(total).isEqualTo(40.0 * 3);
    }

    @Test
    @DisplayName("getPrecioTotal -> si noches < 1 fuerza 1 noche")
    void getPrecioTotal_nochesMin1() {
        Inmueble inm = new Inmueble();
        inm.setPrecioPorNoche(100.0);

        Disponibilidad disp = new Disponibilidad();
        disp.setInmueble(inm);

     
        disp.setFechaInicio(utilDate(2026, 6, 10));
        disp.setFechaFin(utilDate(2026, 6, 10));

        assertThat(disp.getPrecioTotal()).isEqualTo(100.0 * 1);

        
        disp.setFechaInicio(utilDate(2026, 6, 10));
        disp.setFechaFin(utilDate(2026, 6, 9));

        assertThat(disp.getPrecioTotal()).isEqualTo(100.0 * 1);
    }
}
