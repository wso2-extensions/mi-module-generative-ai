package org.wso2.carbon.esb.module.ai.rag;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.esb.module.ai.AbstractAIMediator;


public class DocParser extends AbstractAIMediator {

    @Override
    public void initialize(MessageContext mc) {
    }

    @Override
    public void execute(MessageContext mc) {
        String input = getMediatorParameter(mc, "input", String.class, false);
        String outputProperty = getMediatorParameter(mc, "outputProperty", String.class, false);

        String document = parseDocument(input);
        mc.setProperty(outputProperty, document);
    }

    private String parseDocument(String input) {
        // Currently no parsing is required as we deal with text only for now
        return input;
    }
}
