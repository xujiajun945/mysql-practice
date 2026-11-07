package com.sqlpractice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 三个数据源：
 * - appDataSource：平台自身库（sqlpractice）
 * - roDataSource：只读账号（只能 SELECT）
 * - ddlDataSource：DDL 账号（只对 practice / employees_lab 有写权限）
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource appDataSource(AppProperties props) {
        return build(props.getDatasource().getApp());
    }

    @Bean(name = "roDataSource")
    public DataSource roDataSource(AppProperties props) {
        return build(props.getDatasource().getRo());
    }

    @Bean(name = "ddlDataSource")
    public DataSource ddlDataSource(AppProperties props) {
        return build(props.getDatasource().getDdl());
    }

    @Bean(name = "roJdbcTemplate")
    public JdbcTemplate roJdbcTemplate(@Qualifier("roDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean(name = "ddlJdbcTemplate")
    public JdbcTemplate ddlJdbcTemplate(@Qualifier("ddlDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    private HikariDataSource build(AppProperties.Pool pool) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(pool.getJdbcUrl());
        config.setUsername(pool.getUsername());
        config.setPassword(pool.getPassword());
        config.setMaximumPoolSize(pool.getMaximumPoolSize());
        config.setMinimumIdle(pool.getMinimumIdle());
        config.setPoolName(pool.getPoolName());
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);
        return new HikariDataSource(config);
    }
}
