package com.itq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentServiceApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Document Service API")
                .description("API для работы с документами: создание, перевод по статусам, история изменений, пакетные операции")
                .version("1.0")
                .contact(new Contact()
                    .name("ITQ Group")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local")
            ));
    }
}
