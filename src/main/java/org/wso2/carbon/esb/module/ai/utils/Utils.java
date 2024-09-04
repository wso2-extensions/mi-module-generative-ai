package org.wso2.carbon.esb.module.ai.utils;

import org.apache.synapse.MessageContext;

public class Utils {
    public static final String CONNECTION_NAME = "name";
    public static final String TENANT_INFO_DOMAIN = "tenant.info.domain";

    public static String getConnectionName(MessageContext messageContext) {

        String connectionName = (String) messageContext.getProperty(CONNECTION_NAME);
        return getTenantSpecificConnectionName(connectionName, messageContext);
    }

    public static String getTenantSpecificConnectionName(String connectionName, MessageContext messageContext) {
        Object tenantDomain = messageContext.getProperty(TENANT_INFO_DOMAIN);
        if (tenantDomain != null) {
            return String.format("%s@%s", connectionName, tenantDomain);
        }
        return connectionName;
    }
}
