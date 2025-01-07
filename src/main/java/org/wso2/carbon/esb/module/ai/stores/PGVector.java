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
