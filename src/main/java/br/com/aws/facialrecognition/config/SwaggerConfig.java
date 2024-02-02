package br.com.aws.facialrecognition.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI myOpenAPI() {

        Info info = new Info()
                .title("Facial Recognition Api")
                .version("1.0")
                .description("Facial Recognition");

        return new OpenAPI().info(info);
    }
}