/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.module.ai.stores;

import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.postgresql.Driver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class PGVector extends VectorStore {
        public PGVector(String host, String port, String database, String user, String password, String table, Integer dimension) throws Exception {
                super(PgVectorEmbeddingStore.builder()
                                .host(host)
                                .port(Integer.valueOf(port))
                                .database(database)
                                .user(user)
                                .password(password)
                                .table(table)
                                .createTable(true)
                                .dimension(dimension)
                                .build());
        }

        public static boolean testConnection(String host, Integer port, String db, String user, String password) throws SQLException {
                String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
                Properties props = new Properties();
                props.setProperty("user", user);
                props.setProperty("password", password);

                Driver driver = new org.postgresql.Driver();
                Connection conn = driver.connect(url, props);

                if (conn != null) {
                        conn.close();
                        return true;
                } else {
                        return false;
                }
        }
}
