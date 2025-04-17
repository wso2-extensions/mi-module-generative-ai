/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.database;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.datasource.DataSourceInformation;
import org.apache.synapse.commons.datasource.factory.DataSourceFactory;
import org.apache.synapse.mediators.db.Statement;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.apache.tomcat.dbcp.dbcp2.datasources.PerUserPoolDataSource;
import org.wso2.securevault.secret.SecretInformation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public abstract class RDBMSDatabase {

    protected Log log = LogFactory.getLog(this.getClass());
    private final DataSource dataSource;
    private final String table;

    protected RDBMSDatabase(Builder builder) {

        DataSourceInformation dataSourceInformation = new DataSourceInformation();
        dataSourceInformation.setDriver(getDriver());
        dataSourceInformation.setUrl(getUrl(builder.host, builder.port, builder.database));

        SecretInformation secretInformation = new SecretInformation();
        secretInformation.setUser(builder.user);
        secretInformation.setAliasSecret(builder.password);
        dataSourceInformation.setSecretInformation(secretInformation);

        this.dataSource = DataSourceFactory.createDataSource(dataSourceInformation);
        this.table = builder.table;
    }

    public List<Map<String, Object>> executeSelectQuery(Statement statement) throws SQLException {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = getPreparedStatement(connection, statement);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                results.add(row);
            }
            return results;
        }
    }

    public int executeUpdate(Statement statement) throws SQLException {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = getPreparedStatement(connection, statement)) {
            return preparedStatement.executeUpdate();
        }
    }

    private PreparedStatement getPreparedStatement(Connection connection, Statement statement) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(statement.getRawStatement());
        setParameters(preparedStatement, statement.getParameters());

        return preparedStatement;
    }

    private void setParameters(PreparedStatement preparedStatement, List<Statement.Parameter> parameters)
            throws SQLException {

        int column = 1;
        for (Statement.Parameter parameter : parameters) {
            if (parameter == null) {
                continue;
            }
            String value = parameter.getPropertyName();
            switch (parameter.getType()) {
                // according to J2SE 1.5 /docs/guide/jdbc/getstart/mapping.html
                case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> {
                    if (StringUtils.isNotEmpty(value)) {
                        preparedStatement.setString(column++, value);
                    } else {
                        preparedStatement.setString(column++, null);
                    }
                }
                case Types.NUMERIC, Types.DECIMAL -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setBigDecimal(column++, new BigDecimal(value));
                    } else {
                        preparedStatement.setBigDecimal(column++, null);
                    }
                }
                case Types.BIT -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setBoolean(column++, Boolean.parseBoolean(value));
                    } else {
                        preparedStatement.setNull(column++, Types.BIT);
                    }
                }
                case Types.TINYINT -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setByte(column++, Byte.parseByte(value));
                    } else {
                        preparedStatement.setNull(column++, Types.TINYINT);
                    }
                }
                case Types.SMALLINT -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setShort(column++, Short.parseShort(value));
                    } else {
                        preparedStatement.setNull(column++, Types.SMALLINT);
                    }
                }
                case Types.INTEGER -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setInt(column++, Integer.parseInt(value));
                    } else {
                        preparedStatement.setNull(column++, Types.INTEGER);
                    }
                }
                case Types.BIGINT -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setLong(column++, Long.parseLong(value));
                    } else {
                        preparedStatement.setNull(column++, Types.BIGINT);
                    }
                }
                case Types.REAL -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setFloat(column++, Float.parseFloat(value));
                    } else {
                        preparedStatement.setNull(column++, Types.REAL);
                    }
                }
                case Types.FLOAT -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setDouble(column++, Double.parseDouble(value));
                    } else {
                        preparedStatement.setNull(column++, Types.FLOAT);
                    }
                }
                case Types.DOUBLE -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setDouble(column++, Double.parseDouble(value));
                    } else {
                        preparedStatement.setNull(column++, Types.DOUBLE);
                    }
                }

                // skip BINARY, VARBINARY and LONGVARBINARY
                case Types.DATE -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setDate(column++, Date.valueOf(value));
                    } else {
                        preparedStatement.setNull(column++, Types.DATE);
                    }
                }
                case Types.TIME -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setTime(column++, Time.valueOf(value));
                    } else {
                        preparedStatement.setNull(column++, Types.TIME);
                    }
                }
                case Types.TIMESTAMP -> {
                    if (value != null && value.length() != 0) {
                        preparedStatement.setTimestamp(column++, Timestamp.valueOf(value));
                    } else {
                        preparedStatement.setNull(column++, Types.TIMESTAMP);
                    }
                }
            }

        }
    }

    protected abstract String getDriver();

    protected abstract String getUrl(String host, String port, String database);

    public void shutdown() {

        if (dataSource == null) {
            return;
        }
        if (this.dataSource instanceof BasicDataSource) {
            try {
                ((BasicDataSource) this.dataSource).close();
            } catch (SQLException e) {
                log.warn("Error shutting down DB connection pool for URL : " +
                        ((BasicDataSource) dataSource).getUrl());
            }
        } else if (this.dataSource instanceof PerUserPoolDataSource) {
            ((PerUserPoolDataSource) this.dataSource).close();
            log.info("Successfully shut down DB connection pool for URL : " +
                    ((PerUserPoolDataSource) dataSource).getDataSourceName());
        }
    }

    public String getTable() {

        return table;
    }

    public abstract static class Builder {

        private String host;
        private String port;
        private String user;
        private String password;
        private String database;
        private String table;

        public Builder host(String host) {

            this.host = host;
            return this;
        }

        public Builder port(String port) {

            this.port = port;
            return this;
        }

        public Builder database(String database) {

            this.database = database;
            return this;
        }

        public Builder user(String user) {

            this.user = user;
            return this;
        }

        public Builder password(String password) {

            this.password = password;
            return this;
        }

        public Builder table(String table) {

            this.table = table;
            return this;
        }

        public abstract RDBMSDatabase build();
    }
}
