package com.cho.polio.presentation.tenant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 🌟 [2단계 스키마 라우터]
 * 1단계에서 찾아온 물리 풀(Target DataSource)에서 커넥션을 꺼낸 직후,
 * 현재 테넌트의 스키마로 강제 전환(USE schema)하는 역할을 합니다.
 */
@Slf4j
public class SchemaRoutingDataSource extends DelegatingDataSource {

    private final String schemaName;

    public SchemaRoutingDataSource(DataSource targetDataSource, String schemaName) {
        super(targetDataSource);
        this.schemaName = schemaName;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = super.getConnection();

        // 🎯 커넥션을 반환하기 직전, 스키마 스위칭 쿼리를 날립니다.
        try (Statement stmt = conn.createStatement()) {
            log.info("Switching schema to: {}", schemaName);
            stmt.execute("USE `" + schemaName + "`;");
        } catch (SQLException e) {
            conn.close(); // 스키마 전환 실패 시 커넥션을 안전하게 닫아 자원 누수 방지
            throw e;
        }

        return conn;
    }
}