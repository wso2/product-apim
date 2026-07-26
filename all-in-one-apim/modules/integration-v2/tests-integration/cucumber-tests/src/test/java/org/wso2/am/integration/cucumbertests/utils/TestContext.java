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

import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestContext {

    private static final String DEFAULT_SHARED_SCOPE = "global-shared";
    private static final String DEFAULT_LOCAL_SCOPE = "global-local";

    /**
     * Derives the shared-scope id for a TestNG {@code <test>} block, namespaced by suite name so two
     * blocks of the same {@code <test name>} living in different suites cannot merge shared state.
     * This is the single source of truth for the shared-scope key — listeners that set the scope per
     * invocation and any onStart lifecycle wiring must both derive the key through this method.
     */
    public static String sharedScopeId(ITestContext ctx) {
        return ctx.getSuite().getName() + "::" + ctx.getName();
    }

    private static final Map<String, Map<String, Object>> sharedContexts = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> localContexts = new ConcurrentHashMap<>();
    /**
     * Scope is strictly per-invocation: {@code BlockScopeListener.beforeInvocation} binds it on the very
     * worker thread that runs each invocation (test or config method) and {@code afterInvocation} clears it,
     * so a reused pool thread never carries a stale scope into the next invocation. Deliberately a plain
     * {@link ThreadLocal}, not {@code InheritableThreadLocal}: inheritance is not the isolation mechanism
     * (verified — FV-2.2 {@code BlockScopeThreadVisibilityVerificationTest} passes either way), and a plain
     * ThreadLocal fails <em>closed</em> to the global default rather than silently leaking a stale
     * inherited snapshot. A child thread a step might spawn does NOT inherit the scenario scope and must
     * call {@link #setScope} itself.
     */
    private static final ThreadLocal<ContextScope> currentScope = new ThreadLocal<>();

    private static final class ContextScope {
        private final String sharedScopeId;
        private final String localScopeId;

        private ContextScope(String sharedScopeId, String localScopeId) {
            this.sharedScopeId = sharedScopeId;
            this.localScopeId = localScopeId;
        }
    }

    public static void setScope(String sharedScopeId, String localScopeId) {
        currentScope.set(new ContextScope(defaultIfBlank(sharedScopeId, DEFAULT_SHARED_SCOPE),
                defaultIfBlank(localScopeId, DEFAULT_LOCAL_SCOPE)));
    }

    public static void clearScope() {
        currentScope.remove();
    }

    /**
     * Number of distinct shared-scope maps currently retained. Test-observability only — lets the
     * scope-leak verification assert that finished blocks' entries are reclaimed (no per-block buildup).
     */
    public static int sharedScopeCount() {
        return sharedContexts.size();
    }

    /**
     * Number of distinct local-scope maps currently retained. Test-observability only — lets the
     * scope-leak verification assert that finished blocks' entries are reclaimed (no per-block buildup).
     */
    public static int localScopeCount() {
        return localContexts.size();
    }

    public static void set(String key, Object value) {
        getCurrentLocalContext().put(key, value);
    }

    public static void setShared(String key, Object value) {
        getCurrentSharedContext().put(key, value);
    }

    /**
     * Appends a value to a list stored under the given key, creating the list if absent.
     * Used to register created resources (e.g. API/application ids) for scenario teardown.
     */
    @SuppressWarnings("unchecked")
    public static void addToList(String key, Object value) {
        ((List<Object>) getCurrentLocalContext().computeIfAbsent(key, k -> new ArrayList<>())).add(value);
    }

    /**
     * Returns the list stored under the given key, or an empty list if absent.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(String key) {
        Object value = get(key);
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    public static Object get(String key) {
        Map<String, Object> localContext = getCurrentLocalContext();
        if (localContext.containsKey(key)) {
            return localContext.get(key);
        }
        return getCurrentSharedContext().get(key);
    }

    /**
     * Resolves a REQUIRED value from context: strips an optional {@code <...>} reference wrapper, then reads the
     * key. Throws {@link IllegalArgumentException} if the value is absent — use this for a caller-supplied key
     * that must exist, so a missing/typo'd key fails fast with a clear message instead of a downstream NPE. For a
     * key that may legitimately be absent (or a framework-managed key you null-check yourself), use
     * {@link #get(String)} (nullable) or {@link #contains(String)}.
     */
    public static Object resolve(String key) {
        String lookupKey = (key.startsWith("<") && key.endsWith(">")) ? key.substring(1, key.length() - 1) : key;
        Object value = get(lookupKey);
        if (value == null) {
            throw new IllegalArgumentException("No value found in context for key: " + lookupKey);
        }
        return value;
    }

    public static boolean contains(String key) {
        return getCurrentLocalContext().containsKey(key) || getCurrentSharedContext().containsKey(key);
    }

    public  static void remove(String key) {
        getCurrentLocalContext().remove(key);
    }

    public static void removeShared(String key) {
        getCurrentSharedContext().remove(key);
    }

    public static void clear() {
        ContextScope scope = getScope();
        localContexts.remove(scope.localScopeId);
        sharedContexts.remove(scope.sharedScopeId);
    }

    private static ContextScope getScope() {
        ContextScope scope = currentScope.get();
        if (scope == null) {
            scope = new ContextScope(DEFAULT_SHARED_SCOPE, DEFAULT_LOCAL_SCOPE);
            currentScope.set(scope);
        }
        return scope;
    }

    private static Map<String, Object> getCurrentLocalContext() {
        return localContexts.computeIfAbsent(getScope().localScopeId, key -> new ConcurrentHashMap<>());
    }

    private static Map<String, Object> getCurrentSharedContext() {
        return sharedContexts.computeIfAbsent(getScope().sharedScopeId, key -> new ConcurrentHashMap<>());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
