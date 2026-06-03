package com.faniot.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fanIotOpenApi() {
        String schemeName = "JWT认证";
        return new OpenAPI()
                .info(new Info()
                        .title("工业风机物联网云平台 API")
                        .version("0.0.1")
                        .description("Spring Boot基础框架、用户管理、JWT认证、PostgreSQL、Redis"))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
