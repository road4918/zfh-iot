package com.zfh.iot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class TDengineConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid.tdengine")
    public HikariConfig tdengineHikariConfig() {
        return new HikariConfig();
    }

    @Bean(name = "tdengineDataSource")
    public DataSource tdengineDataSource(@Qualifier("tdengineHikariConfig") HikariConfig config) {
        return new HikariDataSource(config);
    }

    @Bean(name = "tdengineTemplate")
    public JdbcTemplate tdengineTemplate(@Qualifier("tdengineDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
