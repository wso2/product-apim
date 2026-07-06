/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.testcontainers;

import org.apache.commons.logging.Log;
import org.apache.log4j.MDC;
import org.testcontainers.containers.output.OutputFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JclLogConsumer implements Consumer<OutputFrame> {

    private final Log log;
    private String prefix = "";
    private boolean separateOutputStreams = false;
    private final Map<String, String> mdc = new HashMap<>();

    public JclLogConsumer(Log log) {
        this.log = log;
    }

    public JclLogConsumer withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] : ";
        return this;
    }

    public JclLogConsumer withSeparateOutputStreams() {
        this.separateOutputStreams = true;
        return this;
    }

    public JclLogConsumer withMdc(String key, String value) {
        mdc.put(key, value);
        return this;
    }

    @Override
    public void accept(OutputFrame frame) {
        if (frame == null) {
            return;
        }
        OutputFrame.OutputType type = frame.getType();
        if (type == OutputFrame.OutputType.END) {
            return;
        }
        String line = frame.getUtf8String();
        if (line == null) {
            return;
        }
        while (line.endsWith("\n") || line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }
        if (line.isEmpty()) {
            return;
        }

        Map<String, Object> previous = new HashMap<>();
        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            previous.put(entry.getKey(), MDC.get(entry.getKey()));
            MDC.put(entry.getKey(), entry.getValue());
        }
        try {
            if (separateOutputStreams && type == OutputFrame.OutputType.STDERR) {
                log.error(prefix + line);
            } else {
                log.info(prefix + line);
            }
        } finally {
            for (Map.Entry<String, Object> entry : previous.entrySet()) {
                if (entry.getValue() == null) {
                    MDC.remove(entry.getKey());
                } else {
                    MDC.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
