package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config

import org.example.athenajdbcpoc.config.DummyTransactionManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    entityManagerFactoryRef = "athenaEntityManagerFactory",
    transactionManagerRef = "athenaTransactionManager",
    basePackages = ["uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.athena.repository"]
)
class AthenaConfig() {

    @Bean(name = ["athenaDataSource"])
    @ConfigurationProperties(prefix = "spring.datasource.athena")
    fun athenaDataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean(name = ["athenaEntityManagerFactory"])
    fun athenaEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("athenaDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val properties = HashMap<String, Any>()
        properties["hibernate.hbm2ddl.auto"] = "none"
        properties["hibernate.dialect"] = "org.hibernate.dialect.MySQLDialect"
        properties["hibernate.physical_naming_strategy"] = "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        properties["hibernate.temp.use_jdbc_metadata_defaults"] = "false"

        return builder
            .dataSource(dataSource)
            .packages("org.example.athenajdbcpoc.athena.entity")
            .persistenceUnit("athena")
            .properties(properties)
            .build()
    }

    @Bean(name = ["athenaTransactionManager"])
    fun athenaTransactionManager(
    ): PlatformTransactionManager {
        return DummyTransactionManager()
    }
}