/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.utils;

import java.util.HashMap;
import java.util.Map;

public class TestContext {

    // ThreadLocal to maintain a separate contextMap per thread
    private static final ThreadLocal<Map<String, Object>> threadLocalContext =
            ThreadLocal.withInitial(HashMap::new);

    public static void set(String key, Object value) {
        threadLocalContext.get().put(key, value);
    }

    public static Object get(String key) {
        return threadLocalContext.get().get(key);
    }

    public static boolean contains(String key) {
        return threadLocalContext.get().containsKey(key);
    }

    public  static void remove(String key) {
        threadLocalContext.get().remove(key);
    }

    public static void clear() {
        threadLocalContext.get().clear();
    }
}
