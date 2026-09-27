package com.course.server.database;

import com.course.server.config.ServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe Database connection manager using HikariCP connection pool.
 */
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private HikariDataSource dataSource;

    private DatabaseManager() {
        initDataSource();
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    private synchronized void initDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        ServerConfig config = ServerConfig.getInstance();
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getDbUrl());
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize());
        hikariConfig.setMinimumIdle(config.getDbPoolMinIdle());
        hikariConfig.setIdleTimeout(config.getDbPoolIdleTimeout());
        hikariConfig.setConnectionTimeout(config.getDbPoolConnectionTimeout());
        hikariConfig.setPoolName("CourseRegPool");

        // Performance & validation properties
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            LOGGER.info("HikariCP connection pool initialized successfully: " + config.getDbUrl());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize HikariCP connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Obtains a connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initDataSource();
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the connection pool on server shutdown.
     */
    public synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Closing HikariCP connection pool...");
            dataSource.close();
        }
    }
}
