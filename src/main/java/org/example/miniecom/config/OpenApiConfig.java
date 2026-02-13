package org.example.miniecom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Mini E-Commerce API",
                version = "v1",
                description = "REST API for managing products, orders, and order payments.",
                contact = @Contact(name = "Mini Ecom Team"),
                license = @License(name = "Apache 2.0")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local environment")
        },
        tags = {
                @Tag(name = "Products", description = "Catalog product operations"),
                @Tag(name = "Orders", description = "Order lifecycle operations"),
                @Tag(name = "Payments", description = "Order payment operations")
        }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .components(new Components()
                        .addResponses("BadRequest", new ApiResponse()
                                .description("Bad request or validation error")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")))))
                        .addResponses("NotFound", new ApiResponse()
                                .description("Resource not found")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")))))
                        .addResponses("BadGateway", new ApiResponse()
                                .description("Payment gateway error")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))))));
    }
}
