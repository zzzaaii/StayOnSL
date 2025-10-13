package es.uclm.StayOn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("es.uclm.library.persistence") // <--- fuerza escaneo de repositorios
public class StayOnApplication {
    public static void main(String[] args) {
        SpringApplication.run(StayOnApplication.class, args);
    }
}
