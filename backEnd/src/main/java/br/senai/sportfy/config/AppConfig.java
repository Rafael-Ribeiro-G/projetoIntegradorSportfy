package br.senai.sportfy.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.senai.sportfy.SportfyApplication;

@Configuration
public class AppConfig {

    /** Relógio central da aplicação (facilita testes de regras com data/hora). */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(SportfyApplication.FUSO_HORARIO));
    }
}
