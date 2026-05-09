package com.PMS.SP_Estimation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for Spring Boot 4 / Jackson 3.
 *
 * In Jackson 3, WRITE_DATES_AS_TIMESTAMPS was removed from the
 * application.properties binding. We configure it programmatically here
 * to ensure LocalDate and LocalDateTime are serialised as ISO-8601 strings
 * (e.g. "2026-05-09") rather than numeric arrays.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
