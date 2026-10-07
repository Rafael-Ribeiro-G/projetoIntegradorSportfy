package br.senai.sportfy;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SportfyApplication {

    public static final String FUSO_HORARIO = "America/Sao_Paulo";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(FUSO_HORARIO));
        SpringApplication.run(SportfyApplication.class, args);
    }
}
