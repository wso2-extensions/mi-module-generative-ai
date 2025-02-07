package org.wso2.carbon.esb.module.ai;

public enum Errors {
    EMBEDDING_INJECTION_ERROR("AI-EI-0001", "Embedding injection error occurred"),
    PINECONE_CONNECTION_ERROR("AI-PC-0001", "Error creating Pinecone connection"),
    POSTGRE_SQL_CONNECTION_ERROR("AI-PS-0001", "Error creating Postgres connection"),
    EMBEDDING_RETRIEVAL_ERROR("AI-ER-0001", "Embedding retrieval error occurred");

    private final String code;
    private final String message;

    Errors(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
