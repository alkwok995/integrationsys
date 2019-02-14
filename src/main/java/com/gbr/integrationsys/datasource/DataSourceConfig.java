package com.gbr.integrationsys.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Value("${sites}")
    private String sites;

    @Autowired
    private DbProperties dbProperties;

    @Autowired
    private Environment env;

    @Bean(name = "dataSource")
    public DataSource dataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        for (String site : dbProperties.getHikari().keySet()) {
            targetDataSources.put(site, dbProperties.getHikari().get(site));
        }

        HikariDataSource defaultDataSource = dbProperties.getHikari().get("default");

        DynamicDataSource dataSource = new DynamicDataSource();
        dataSource.setTargetDataSources(targetDataSources);
        dataSource.setDefaultTargetDataSource(defaultDataSource);

        return dataSource;
    }

    @Bean
    public PlatformTransactionManager txManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    private HikariDataSource genDataSource(String site) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("hikari." + site + ".jdbc-url"));
        config.setUsername(env.getProperty("hikari." + site + ".username"));
        config.setPassword(env.getProperty("hikari." + site + ".password"));
        return new HikariDataSource(config);
    }
}
