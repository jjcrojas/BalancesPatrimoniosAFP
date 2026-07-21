package co.gov.sfc.balancesafppatrimonios.config;

import com.p6spy.engine.spy.P6DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    /*
     * Pool real de conexiones hacia Teradata.
     */
    @Bean(
            name = "teradataHikariDataSource",
            destroyMethod = "close"
    )
    public HikariDataSource teradataHikariDataSource(
            BalancesAfpPatrimoniosProperties properties) {

        BalancesAfpPatrimoniosProperties.Datasource source =
                properties.getDatasource();

        HikariConfig config =
                new HikariConfig();

        config.setJdbcUrl(
                source.getUrl()
        );

        config.setDriverClassName(
                source.getDriverClassName()
        );

        config.setUsername(
                source.getUsername()
        );

        config.setPassword(
                source.getPassword()
        );

        config.setMaximumPoolSize(
                source.getMaximumPoolSize()
        );

        config.setMinimumIdle(
                source.getMinimumIdle()
        );

        /*
         * Tiempo máximo para obtener una conexión
         * disponible del pool. Está expresado
         * en milisegundos.
         */
        config.setConnectionTimeout(
                source.getConnectionTimeout()
        );

        config.setValidationTimeout(
                source.getValidationTimeout()
        );

        config.setInitializationFailTimeout(
                source.getInitializationFailTimeout()
        );

        config.setPoolName(
                "BalancesAFPPatrimoniosPool"
        );

        config.setReadOnly(true);

        return new HikariDataSource(config);
    }

    /*
     * P6Spy envuelve el DataSource real para registrar
     * los SQL con sus parámetros.
     */
    @Bean(name = "balancesAfpPatrimoniosDataSource")
    public DataSource balancesAfpPatrimoniosDataSource(
            @Qualifier("teradataHikariDataSource")
            HikariDataSource hikariDataSource) {

        return new P6DataSource(
                hikariDataSource
        );
    }

    /*
     * JdbcTemplate utilizado por el repositorio.
     */
    @Bean(name = "balancesAfpPatrimoniosJdbcTemplate")
    public JdbcTemplate balancesAfpPatrimoniosJdbcTemplate(
            @Qualifier("balancesAfpPatrimoniosDataSource")
            DataSource dataSource,

            BalancesAfpPatrimoniosProperties properties) {

        JdbcTemplate template =
                new JdbcTemplate(dataSource);

        template.setFetchSize(2_000);

        /*
         * Antes estaba fijo:
         *
         * template.setQueryTimeout(180);
         *
         * Ahora se toma del archivo de propiedades.
         */
        template.setQueryTimeout(
                properties.getDatasource()
                        .getQueryTimeoutSeconds()
        );

        return template;
    }
}