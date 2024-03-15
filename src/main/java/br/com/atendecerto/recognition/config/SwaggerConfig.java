package br.com.atendecerto.recognition.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI myOpenAPI() {

        Info info = new Info()
                .title("Rekognition")
                .version("1.0")
                .description("Rekognition");

        return new OpenAPI().info(info);
    }
}