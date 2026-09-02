/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.wso2.am.testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds one distributed component TOML without copying a developer full-file configuration. */
public final class DistributedApimTomlBuilder {

    private static final ObjectMapper TOML = new TomlMapper();

    private DistributedApimTomlBuilder() {
    }

    public enum Component {
        CP("cp"),
        TM("tm"),
        GATEWAY("gateway");

        private final String parameterName;

        Component(String parameterName) {
            this.parameterName = parameterName;
        }

        public String parameterName() {
            return parameterName;
        }
    }

    /**
     * Merge order is product defaults, distributed base, component extra, then generated runtime values.
     * Runtime values are deliberately last so a user overlay cannot redirect a component to localhost.
     */
    public static String build(String productDefaults, String distributedBaseOverlay,
                               String componentOverlay, String extraOverlay,
                               Map<String, ?> finalRuntimeValues) throws IOException {
        ObjectNode result = parse(productDefaults, "product defaults");
        merge(result, parse(distributedBaseOverlay, "distributed base overlay"));
        if (componentOverlay != null && !componentOverlay.isBlank()) {
            merge(result, parse(componentOverlay, "component overlay"));
        }
        if (extraOverlay != null && !extraOverlay.isBlank()) {
            merge(result, parse(extraOverlay, "component extra overlay"));
        }
        applyRuntimeValues(result, finalRuntimeValues == null ? Collections.emptyMap() : finalRuntimeValues);
        return TOML.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /** Resolves only a qualified extra overlay; distributed mode rejects the ambiguous legacy key. */
    public static String resolveExtraOverlay(Map<String, String> parameters, Component component)
            throws IOException {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(component, "component");
        String legacy = parameters.get("tomlExtraOverlayPath");
        if (legacy != null && !legacy.isBlank()) {
            throw new IllegalArgumentException("Distributed APIM requires a qualified overlay parameter: "
                    + "tomlExtraOverlayPath." + component.parameterName());
        }
        return readOptional(parameters.get("tomlExtraOverlayPath." + component.parameterName()));
    }

    /** Resolves boot files for one component and rejects cross-component ambiguity. */
    public static List<ServerFile> resolveServerFiles(Map<String, String> parameters, Component component)
            throws IOException {
        Objects.requireNonNull(parameters, "parameters");
        String legacy = parameters.get("serverFilesToCopy");
        if (legacy != null && !legacy.isBlank()) {
            throw new IllegalArgumentException("Distributed APIM requires qualified server files: "
                    + "serverFilesToCopy." + component.parameterName());
        }
        String value = parameters.get("serverFilesToCopy." + component.parameterName());
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<ServerFile> files = new ArrayList<>();
        for (String entry : value.split(",")) {
            String[] parts = entry.trim().split("::", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("Malformed serverFilesToCopy." + component.parameterName()
                        + " entry '" + entry + "'; expected <hostPath>::<serverRelativePath>");
            }
            Path source = Path.of(parts[0].trim()).normalize();
            if (!Files.isRegularFile(source)) {
                throw new IllegalArgumentException("Server file does not exist: " + source);
            }
            files.add(new ServerFile(source, parts[1].trim()));
        }
        return List.copyOf(files);
    }

    public static final class ServerFile {
        private final Path source;
        private final String serverRelativePath;

        public ServerFile(Path source, String serverRelativePath) {
            this.source = source;
            this.serverRelativePath = serverRelativePath;
        }

        public Path source() {
            return source;
        }

        public String serverRelativePath() {
            return serverRelativePath;
        }
    }

    private static String readOptional(String path) throws IOException {
        return path == null || path.isBlank() ? null : Files.readString(Path.of(path).normalize());
    }

    private static ObjectNode parse(String content, String description) throws IOException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(description + " must not be empty");
        }
        JsonNode node = TOML.readTree(content);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(description + " must contain a TOML table");
        }
        return (ObjectNode) node;
    }

    private static void merge(ObjectNode target, ObjectNode overlay) {
        overlay.fields().forEachRemaining(field -> {
            JsonNode incoming = field.getValue();
            JsonNode existing = target.get(field.getKey());
            if (existing != null && existing.isObject() && incoming.isObject()) {
                merge((ObjectNode) existing, (ObjectNode) incoming);
            } else {
                target.set(field.getKey(), incoming);
            }
        });
    }

    private static void applyRuntimeValues(ObjectNode target, Map<String, ?> values) {
        values.forEach((path, value) -> {
            if (path == null || path.isBlank() || value == null) {
                throw new IllegalArgumentException("Runtime TOML keys and values must be non-empty");
            }
            String[] segments = path.split("\\.");
            ObjectNode cursor = target;
            for (int i = 0; i < segments.length - 1; i++) {
                JsonNode child = cursor.get(segments[i]);
                if (child != null && !child.isObject()) {
                    throw new IllegalArgumentException("Runtime TOML path crosses a scalar: " + path);
                }
                cursor = child == null ? cursor.putObject(segments[i]) : (ObjectNode) child;
            }
            cursor.set(segments[segments.length - 1], TOML.valueToTree(value));
        });
    }
}
