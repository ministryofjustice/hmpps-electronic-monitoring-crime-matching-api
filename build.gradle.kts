plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.1.0"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "1.9.0"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.3")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.4")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
  implementation("software.amazon.awssdk:athena:2.31.45")
  implementation("org.json:json:20250517")
  implementation("io.zeko:zeko-sql-builder:1.5.6")

  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")

  testImplementation("com.h2database:h2:2.3.232")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.4.3")
  testImplementation("org.mockito:mockito-core:5.17.0")
  testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
  testImplementation("org.wiremock:wiremock-standalone:3.13.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.28") {
    exclude(group = "io.swagger.core.v3")
  }
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
