package com.serviceOrder.Management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Helpdesk API - Gestão de Ordens de Serviço")
                        .version("1.0.0")
                        .description("API RESTful para gerenciamento de clientes, técnicos e ordens de serviço.")
                        .contact(new Contact()
                                .name("Suporte Técnico")
                                .email("suporte@helpdesk.com")));
    }
}
