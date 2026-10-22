package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version!!
  private val securitySchemeName = "bearer-jwt"

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://electronic-monitoring-crime-matching-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("https://electronic-monitoring-crime-matching-api-preprod.hmpps.service.justice.gov.uk").description("Pre-Production"),
        Server().url("https://electronic-monitoring-crime-matching-api.hmpps.service.justice.gov.uk").description("Production"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .components(
      io.swagger.v3.oas.models.Components()
        .addSecuritySchemes(
          securitySchemeName,
          SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")
            .description("A HMPPS Auth access token. Check that it has the required role(s) for the endpoints being called."),
        ),
    )
    .security(
      listOf(
        SecurityRequirement().addList(securitySchemeName),
      ),
    )
    .tags(
      listOf(),
    )
    .info(
      Info().title("HMPPS Electronic Monitoring Crime Matching Api").version(version)
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
}
