package org.wso2.carbon.esb.module.ai;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;

import java.util.Objects;

/**
 * Abstract base class for AI mediators
 */
public abstract class AbstractAIMediator extends AbstractConnector {

    // This method needs to be implemented by every AI mediator
    abstract public void execute(MessageContext messageContext);

    @Override
    public void connect(MessageContext messageContext){
        execute(messageContext);
    }

    protected <T> T getMediatorParameter(MessageContext messageContext, String parameterName, Class<T> type, boolean isOptional) {
        Object parameter = getParameter(messageContext, parameterName);
        if (parameter == null && !isOptional) {
            handleException(String.format("Parameter %s is not provided", parameterName), messageContext);
        } else if (parameter == null) {
            return null;
        }

        try {
            return parse(Objects.requireNonNull(parameter).toString(), type);
        } catch (IllegalArgumentException e) {
            handleException(String.format("Parameter %s is not of type %s", parameterName, type.getName()), messageContext);
        }

        return null;
    }

    protected <T> T getProperty(MessageContext messageContext, String propertyName, Class<T> type, boolean isOptional) {
        Object property = messageContext.getProperty(propertyName);
        if (property == null && !isOptional) {
            handleException(String.format("Property %s is not set", propertyName), messageContext);
        } else if (property == null) {
            return null;
        }

        try {
            return parse(Objects.requireNonNull(property).toString(), type);
        } catch (IllegalArgumentException e) {
            handleException(String.format("Property %s is not of type %s", propertyName, type.getName()), messageContext);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(String value, Class<T> type) throws IllegalArgumentException {
        if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (type == Double.class) {
            return (T) Double.valueOf(value);
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (type == String.class) {
            return (T) value;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
}
