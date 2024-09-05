package org.wso2.carbon.esb.module.ai;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;

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

        if (type.isInstance(parameter)) {
            return type.cast(parameter); // Cast safely using Class<T>
        } else {
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

        if (type.isInstance(property)) {
            return type.cast(property); // Cast safely using Class<T>
        } else {
            handleException(String.format("Property %s is not of type %s", propertyName, type.getName()), messageContext);
        }

        return null;
    }
}
