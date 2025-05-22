package org.wso2.am.integration.cucumbertests;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class MessageContentFilter extends Filter<ILoggingEvent> {
    @Override
    public FilterReply decide(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        if (msg != null && (msg.contains("WARN") || msg.contains("ERROR"))) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }
}
