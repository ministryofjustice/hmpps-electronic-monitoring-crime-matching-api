plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.3"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
  implementation("software.amazon.awssdk:athena:2.41.28")
  implementation("software.amazon.awssdk:s3:2.41.28")
  implementation("com.sun.mail:jakarta.mail:2.0.2")
  implementation("org.apache.commons:commons-csv:1.14.1")
  implementation("org.json:json:20251224")
  implementation("io.zeko:zeko-sql-builder:1.5.6")
  implementation("uk.gov.service.notify:notifications-java-client:6.0.0-RELEASE")
  implementation("org.locationtech.proj4j:proj4j:1.2.3")
  implementation("org.locationtech.proj4j:proj4j-epsg:1.2.3")

  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("com.h2database:h2:2.4.240")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.mockito:mockito-core:5.21.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  testImplementation("org.wiremock:wiremock-standalone:3.13.2")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("org.skyscreamer:jsonassert:1.5.3")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
  }

  withType<Test> {
    finalizedBy("jacocoTestReport") // report is always generated after tests run
  }

  named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")

    reports { html.required.set(true) }

    classDirectories.setFrom(fileTree(projectDir) { include("build/classes/kotlin/main/**") })
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(files("build/jacoco/test.exec"))
  }
}
