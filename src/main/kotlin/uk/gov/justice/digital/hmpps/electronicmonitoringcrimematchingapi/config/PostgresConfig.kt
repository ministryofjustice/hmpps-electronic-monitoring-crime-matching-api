package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "postgresEntityManagerFactory",
    transactionManagerRef = "postgresTransactionManager",
    basePackages = ["uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.postgres.repository"]
)
class PostgresConfig {

    @Primary
    @Bean(name = ["postgresDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    fun postgresDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Primary
    @Bean(name = ["postgresEntityManagerFactory"])
    fun postgresEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("postgresDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(dataSource)
            .packages("org.example.athenajdbcpoc.postgres.entity")
            .persistenceUnit("postgres")
            .build()
    }

    @Primary
    @Bean(name = ["postgresTransactionManager"])
    fun postgresTransactionManager(
        @Qualifier("postgresEntityManagerFactory") entityManagerFactory: LocalContainerEntityManagerFactoryBean
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory.getObject()!!)
    }

}