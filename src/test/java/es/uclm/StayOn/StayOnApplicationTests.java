package es.uclm.StayOn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class ServletInitializerTest {

    @Test
    void configure_setsSources() {
        ServletInitializer initializer = new ServletInitializer();

        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        SpringApplicationBuilder configured = initializer.configure(builder);

        assertThat(configured).isNotNull();
        
    }
}
