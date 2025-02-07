package org.wso2.carbon.esb.module.ai.exception;

import org.wso2.carbon.esb.module.ai.Errors;

public class VectorStoreException extends RuntimeException {
    Errors error;

    public VectorStoreException(Errors error, Throwable cause) {
        super(error.getMessage());
        this.error = error;
        this.initCause(cause);
    }

    public VectorStoreException(Errors error) {
        super(error.getMessage());
    }

    public Errors getError() {
        return error;
    }
}
