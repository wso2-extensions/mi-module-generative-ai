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

package org.wso2.carbon.esb.module.ai.memory.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.db.Statement;
import org.wso2.carbon.esb.module.ai.database.RDBMSDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RDBMSChatMemoryStore implements DatabaseChatMemoryStore {

    protected Log log = LogFactory.getLog(this.getClass());
    private RDBMSDatabase database;

    public RDBMSChatMemoryStore(RDBMSDatabase database) {

        this.database = database;
        createTable();
    }

    private boolean createTable() {

        try {
            String createTable =
                    "CREATE TABLE IF NOT EXISTS " + database.getTable() + " (" +
                            "id SERIAL PRIMARY KEY, " +
                            "userID TEXT NOT NULL, " +
                            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "message TEXT NOT NULL" +
                            ")";
            Statement statement = new Statement(createTable);
            database.executeUpdate(statement);
            return true;
        } catch (SQLException exception) {
            log.error("Error while creating table", exception);
        }
        return false;
    }

    @Override
    public List<ChatMessage> getMessages(Object o) {

        if (o == null) {
            return null;
        }
        String userID = String.valueOf(o);
        String selectQuery = "SELECT * FROM " + database.getTable() + " WHERE userID = ? ORDER BY timestamp DESC";
        Statement selectStatement = new Statement(selectQuery);
        selectStatement.addParameter(userID, null, "VARCHAR");
        try {
            List<Map<String, Object>> resultSet = database.executeSelectQuery(selectStatement);
            if (resultSet != null) {
                return parseChatMessages(resultSet);
            }
        } catch (SQLException exception) {
            log.error("Error while getting messages", exception);
        }
        return null;
    }

    @Override
    public List<ChatMessage> getMessages(Object o, int limit) {

        if (o == null) {
            return null;
        }
        String userID = String.valueOf(o);
        String selectQuery =
                "SELECT * FROM " + database.getTable() + " WHERE userID = ? ORDER BY timestamp DESC LIMIT ?";
        Statement selectStatement = new Statement(selectQuery);
        selectStatement.addParameter(userID, null, "VARCHAR");
        selectStatement.addParameter(String.valueOf(limit), null, "INTEGER");
        try {
            List<Map<String, Object>> resultSet = database.executeSelectQuery(selectStatement);
            if (resultSet != null) {
                return parseChatMessages(resultSet);
            }
        } catch (SQLException exception) {
            log.error("Error while getting messages", exception);
        }
        return null;
    }

    private List<ChatMessage> parseChatMessages(List<Map<String, Object>> resultSet) {

        List<ChatMessage> chatMessages = new ArrayList<>();

        for (Map<String, Object> row : resultSet) {
            String messageJson = (String) row.get("message");
            ChatMessage message = ChatMessageDeserializer.messageFromJson(messageJson);
            chatMessages.add(0, message);
        }
        return chatMessages;
    }

    @Override
    public void updateMessages(Object o, List<ChatMessage> list) {

        throw new UnsupportedOperationException("This is not supported for database memory store");
    }

    /**
     * Update messages in the database
     *
     * @param o       userID
     * @param message message to be updated
     */
    @Override
    public int updateMessage(Object o, ChatMessage message) {

        if (o == null || message == null) {
            return -1;
        }

        String userID = String.valueOf(o);
        String messageContent = ChatMessageSerializer.messageToJson(message);

        String insertQuery = "INSERT INTO " + database.getTable() +
                " (userID, timestamp, message) VALUES (?, NOW(), ?)";
        Statement insertStatement = new Statement(insertQuery);
        insertStatement.addParameter(userID, null, "VARCHAR");
        insertStatement.addParameter(messageContent, null, "LONGVARCHAR");
        try {
            return database.executeUpdate(insertStatement);
        } catch (SQLException exception) {
            log.error("Error while updating messages", exception);
        }
        return -1;
    }

    @Override
    public void deleteMessages(Object o) {

        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void shutdown() {

        database.shutdown();
    }
}
