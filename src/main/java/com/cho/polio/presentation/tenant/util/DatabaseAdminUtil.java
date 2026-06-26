package com.cho.polio.presentation.tenant.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DatabaseAdminUtil {

    public void createDatabase(String url, String username, String password, String schemaName) {
        String sql = "CREATE DATABASE IF NOT EXISTS `" + schemaName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        String connectionUrl = formatBaseUrl(url);

        try (Connection conn = DriverManager.getConnection(connectionUrl, username, password);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            log.info("Database '{}' created or already exists on target URL: {}", schemaName, url);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database '" + schemaName + "' on " + url, e);
        }
    }

    public List<String> extractSchemaDdl(String url, String schemaName, String username, String password) {
        String targetUrlWithSchema = formatSchemaUrl(url, schemaName);
        List<String> ddlStatements = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(targetUrlWithSchema, username, password);
                Statement stmt = conn.createStatement()) {

            log.debug("DDL Extraction Target DB: {}, Catalog: {}", conn.getMetaData().getURL(), conn.getCatalog());

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }

            if (tables.isEmpty()) {
                log.warn("Warning: No tables found in reference schema '{}'.", schemaName);
            }

            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
                    if (rs.next()) {
                        ddlStatements.add(rs.getString(2));
                    }
                }
            }
            log.info("Extracted {} table DDLs from reference schema '{}'", ddlStatements.size(), schemaName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to extract schema DDL from schema '" + schemaName + "'", e);
        }
        return ddlStatements;
    }

    public void executeDdlOnDatabase(String url, String schemaName, String username, String password, List<String> ddlStatements) {
        String connectionUrl = formatSchemaUrl(url, schemaName);

        try (Connection conn = DriverManager.getConnection(connectionUrl, username, password);
                Statement stmt = conn.createStatement()) {

            int successCount = 0;
            int skipCount = 0;

            for (String ddl : ddlStatements) {
                try {
                    String modifiedDdl = ddl.replaceFirst("(?i)CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
                    stmt.executeUpdate(modifiedDdl);
                    successCount++;
                } catch (SQLException e) {
                    if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                        skipCount++;
                    } else {
                        throw e;
                    }
                }
            }
            log.info("DDL execution on '{}' completed: {} executed, {} skipped", schemaName, successCount, skipCount);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute DDL on database '" + schemaName + "'", e);
        }
    }

    /* ================= 헬퍼 유틸리티 메서드 ================= */

    private String formatBaseUrl(String url) {
        if (url.contains("?")) {
            url = url.split("\\?")[0];
        }
        if (url.matches(".*:\\d+/.*")) {
            url = url.substring(0, url.lastIndexOf("/"));
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private String formatSchemaUrl(String url, String schemaName) {
        String params = "";
        if (url.contains("?")) {
            params = url.substring(url.indexOf("?"));
        }
        return formatBaseUrl(url) + schemaName + params;
    }
}