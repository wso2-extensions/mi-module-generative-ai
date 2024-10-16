package org.wso2.carbon.esb.module.ai.utils;

import com.google.gson.Gson;
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

    public static String toJSONString(Object object) {
        return new Gson().toJson(object);
    }

    public static <T> T fromJSONString(String json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }
}