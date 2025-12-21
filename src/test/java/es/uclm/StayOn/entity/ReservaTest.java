package es.uclm.StayOn.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ReservaTest {

    private static Date utilFromLocal(LocalDate ld) {
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("isActiva -> true si hoy está entre inicio y fin (inclusive)")
    void isActiva_true_insideRange() {
        Reserva r = new Reserva();
        LocalDate today = LocalDate.now();

        r.setFechaInicio(utilFromLocal(today.minusDays(1)));
        r.setFechaFin(utilFromLocal(today.plusDays(1)));

        assertThat(r.isActiva()).isTrue();
    }

    @Test
    @DisplayName("isActiva -> false si hoy es antes de fechaInicio")
    void isActiva_false_beforeStart() {
        Reserva r = new Reserva();
        LocalDate today = LocalDate.now();

        r.setFechaInicio(utilFromLocal(today.plusDays(1)));
        r.setFechaFin(utilFromLocal(today.plusDays(3)));

        assertThat(r.isActiva()).isFalse();
    }

    @Test
    @DisplayName("isActiva -> false si hoy es después de fechaFin")
    void isActiva_false_afterEnd() {
        Reserva r = new Reserva();
        LocalDate today = LocalDate.now();

        r.setFechaInicio(utilFromLocal(today.minusDays(3)));
        r.setFechaFin(utilFromLocal(today.minusDays(1)));

        assertThat(r.isActiva()).isFalse();
    }

    @Test
    @DisplayName("getDireccion -> null si inmueble es null, si no devuelve direccion")
    void getDireccion_branch() {
        Reserva r = new Reserva();

        r.setInmueble(null);
        assertThat(r.getDireccion()).isNull();

        Inmueble inm = new Inmueble();
        inm.setDireccion("Calle Falsa 123");
        r.setInmueble(inm);

        assertThat(r.getDireccion()).isEqualTo("Calle Falsa 123");
    }

    @Test
    @DisplayName("getNoches -> 0 si fechas null")
    void getNoches_nullDates_returns0() {
        Reserva r = new Reserva();
        r.setFechaInicio(null);
        r.setFechaFin(null);
        assertThat(r.getNoches()).isEqualTo(0);
    }

    @Test
    @DisplayName("getNoches -> usa rama java.sql.Date cuando ambas son sql.Date")
    void getNoches_sqlDateBranch() {
        Reserva r = new Reserva();

        java.sql.Date ini = java.sql.Date.valueOf(LocalDate.of(2026, 6, 1));
        java.sql.Date fin = java.sql.Date.valueOf(LocalDate.of(2026, 6, 4)); // 3 días

        r.setFechaInicio(ini);
        r.setFechaFin(fin);

        assertThat(r.getNoches()).isEqualTo(3);
    }

    @Test
    @DisplayName("getNoches -> usa rama util.Date cuando no son sql.Date")
    void getNoches_utilDateBranch() {
        Reserva r = new Reserva();

        r.setFechaInicio(utilFromLocal(LocalDate.of(2026, 6, 1)));
        r.setFechaFin(utilFromLocal(LocalDate.of(2026, 6, 4))); // 3 días

        assertThat(r.getNoches()).isEqualTo(3);
    }

    @Test
    @DisplayName("getNoches -> si noches < 1 devuelve 1")
    void getNoches_min1() {
        Reserva r = new Reserva();

        // mismas fechas => between = 0 => 1
        r.setFechaInicio(utilFromLocal(LocalDate.of(2026, 6, 10)));
        r.setFechaFin(utilFromLocal(LocalDate.of(2026, 6, 10)));

        assertThat(r.getNoches()).isEqualTo(1);
    }
}
