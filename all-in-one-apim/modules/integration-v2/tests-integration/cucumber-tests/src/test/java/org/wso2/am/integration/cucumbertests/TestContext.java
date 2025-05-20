package org.wso2.am.integration.cucumbertests;

import java.util.HashMap;
import java.util.Map;

public class TestContext {

    // ThreadLocal to maintain a separate contextMap per thread
    private static final ThreadLocal<Map<String, Object>> threadLocalContext =
            ThreadLocal.withInitial(HashMap::new);

    public void set(String key, Object value) {
        threadLocalContext.get().put(key, value);
    }

    public Object get(String key) {
        return threadLocalContext.get().get(key);
    }

    public boolean contains(String key) {
        return threadLocalContext.get().containsKey(key);
    }

    public void remove(String key) {
        threadLocalContext.get().remove(key);
    }

    public void clear() {
        threadLocalContext.get().clear();
    }
}
