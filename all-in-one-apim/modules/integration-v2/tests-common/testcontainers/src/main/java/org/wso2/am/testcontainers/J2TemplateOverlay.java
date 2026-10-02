/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package org.wso2.am.testcontainers;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Applies an XML overlay to a product Jinja ({@code .j2}) configuration template, the template counterpart of the
 * TOML overlays merged onto {@code deployment.toml}.
 *
 * <p>The overlay is a well-formed XML skeleton that restates only the ancestors of each value it sets, e.g.
 * {@code <wso2registry><enableCache>false</enableCache></wso2registry>} addresses {@code /wso2registry/enableCache}.
 * Only leaf text values are replaced; an overlay cannot add or remove elements.</p>
 *
 * <p>A template is not XML until it is rendered (Jinja appears inside tags and as tag names), so it is read through
 * a length- and newline-preserving mask: {@code {% ... %}} becomes spaces and {@code {{ ... }}} becomes {@code x}s.
 * The JDK StAX parser then resolves element paths on the masked text, skipping XML comments, and each value is
 * replaced at its offset in the ORIGINAL template, so the result is byte-identical apart from the replaced values.</p>
 *
 * <p>Every overlay path must match exactly one element. A path that matches nothing (the template changed), more
 * than one element (e.g. the same element in both branches of a Jinja conditional), or an element that is not a
 * plain-text leaf fails with {@link IllegalArgumentException} instead of being skipped or guessed.</p>
 */
public final class J2TemplateOverlay {

    private static final Pattern JINJA_STATEMENT = Pattern.compile("\\{%.*?%\\}", Pattern.DOTALL);
    private static final Pattern JINJA_EXPRESSION = Pattern.compile("\\{\\{.*?\\}\\}", Pattern.DOTALL);

    private J2TemplateOverlay() {
    }

    /**
     * Returns {@code template} with every leaf value of {@code overlay} applied.
     *
     * @param template the product template content
     * @param overlay  the XML overlay content
     * @return the template with the overlay's values substituted
     * @throws IllegalArgumentException if the overlay is malformed or a path does not match exactly one leaf
     */
    public static String merge(String template, String overlay) {
        Map<String, String> values = readOverlayLeaves(overlay);
        String masked = mask(template);
        Map<String, List<int[]>> regions = locateLeafRegions(masked, values);

        List<Replacement> replacements = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            List<int[]> found = regions.getOrDefault(key, List.of());
            if (found.size() != 1) {
                throw new IllegalArgumentException("Template overlay path " + key + " must match exactly one "
                        + "element in the template, but matched " + found.size());
            }
            int[] region = found.get(0);
            if (region[0] < 0) {
                throw new IllegalArgumentException("Template overlay path " + key
                        + " addresses an element that is not a plain-text leaf");
            }
            if (!template.substring(region[0], region[1]).equals(masked.substring(region[0], region[1]))) {
                throw new IllegalArgumentException("Template overlay path " + key + " addresses a value rendered "
                        + "from deployment.toml; set it with a deployment.toml overlay instead");
            }
            replacements.add(new Replacement(region[0], region[1], escape(entry.getValue())));
        }
        replacements.sort((a, b) -> Integer.compare(b.start(), a.start()));
        StringBuilder result = new StringBuilder(template);
        for (Replacement replacement : replacements) {
            result.replace(replacement.start(), replacement.end(), replacement.value());
        }
        return result.toString();
    }

    private static final class Replacement {

        private final int start;
        private final int end;
        private final String value;

        private Replacement(int start, int end, String value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }

        private int start() {
            return start;
        }

        private int end() {
            return end;
        }

        private String value() {
            return value;
        }
    }

    /** Replaces Jinja statements with spaces and expressions with {@code x}s, keeping every newline in place. */
    static String mask(String template) {
        return maskEach(maskEach(template, JINJA_STATEMENT, ' '), JINJA_EXPRESSION, 'x');
    }

    private static String maskEach(String text, Pattern pattern, char fill) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder masked = new StringBuilder(text);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (masked.charAt(i) != '\n' && masked.charAt(i) != '\r') {
                    masked.setCharAt(i, fill);
                }
            }
        }
        return masked.toString();
    }

    private static Map<String, String> readOverlayLeaves(String overlay) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            XMLStreamReader reader = newReader(overlay);
            Deque<String> path = new ArrayDeque<>();
            Deque<Boolean> hasChildElement = new ArrayDeque<>();
            StringBuilder text = new StringBuilder();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (!hasChildElement.isEmpty()) {
                        hasChildElement.pop();
                        hasChildElement.push(true);
                    }
                    path.addLast(reader.getLocalName());
                    hasChildElement.push(false);
                    text.setLength(0);
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    text.append(reader.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String key = "/" + String.join("/", path);
                    boolean parent = hasChildElement.pop();
                    if (!parent) {
                        String value = text.toString().trim();
                        if (value.isEmpty()) {
                            throw new IllegalArgumentException("Template overlay leaf " + key + " has no value");
                        }
                        if (values.put(key, value) != null) {
                            throw new IllegalArgumentException("Template overlay sets " + key + " more than once");
                        }
                    }
                    path.removeLast();
                    text.setLength(0);
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Template overlay is not well-formed XML: " + e.getMessage(), e);
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Template overlay sets no values");
        }
        return values;
    }

    /**
     * Finds, for each requested path, the [start, end) region of every element at that path whose content is plain
     * text only. The region is the text between the start and end tags, excluding surrounding whitespace.
     */
    private static Map<String, List<int[]>> locateLeafRegions(String masked, Map<String, String> wanted) {
        Map<String, List<int[]>> regions = new LinkedHashMap<>();
        try {
            XMLStreamReader reader = newReader(masked);
            Deque<String> path = new ArrayDeque<>();
            String openKey = null;
            int contentStart = -1;
            boolean plainText = false;
            StringBuilder parsedText = new StringBuilder();
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (openKey != null) {
                        regions.computeIfAbsent(openKey, k -> new ArrayList<>()).add(new int[] {-1, -1});
                        openKey = null;
                    }
                    path.addLast(reader.getLocalName());
                    String key = "/" + String.join("/", path);
                    if (wanted.containsKey(key)) {
                        openKey = key;
                        contentStart = startTagEnd(masked, key, reader.getLocalName(),
                                reader.getLocation().getCharacterOffset());
                        plainText = true;
                        parsedText.setLength(0);
                    }
                } else if (openKey != null && (event == XMLStreamConstants.CHARACTERS
                        || event == XMLStreamConstants.SPACE)) {
                    parsedText.append(reader.getText());
                } else if (openKey != null && (event == XMLStreamConstants.COMMENT
                        || event == XMLStreamConstants.CDATA
                        || event == XMLStreamConstants.PROCESSING_INSTRUCTION
                        || event == XMLStreamConstants.ENTITY_REFERENCE)) {
                    plainText = false;
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if (openKey != null) {
                        List<int[]> found = regions.computeIfAbsent(openKey, k -> new ArrayList<>());
                        if (plainText) {
                            found.add(trimmedRegion(masked, openKey, contentStart, parsedText.toString()));
                        } else {
                            found.add(new int[] {-1, -1});
                        }
                        openKey = null;
                    }
                    path.removeLast();
                }
            }
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Template is not well-formed once Jinja is masked: "
                    + e.getMessage(), e);
        }
        return regions;
    }

    /**
     * Returns the offset just after the start tag the parser has read. The JDK parser reports that position for a
     * START_ELEMENT event; it is verified against the masked text so a different behaviour fails loudly.
     */
    private static int startTagEnd(String masked, String key, String localName, int offset) {
        int tagStart = offset > 0 ? masked.lastIndexOf('<', offset - 1) : -1;
        boolean resolved = tagStart >= 0 && masked.charAt(offset - 1) == '>'
                && masked.startsWith("<" + localName, tagStart);
        if (!resolved) {
            throw new IllegalStateException("Could not resolve the position of " + key
                    + " in the template (offset " + offset + ")");
        }
        return offset;
    }

    /** Cross-checks the raw region against the parser's text, then trims surrounding whitespace off the region. */
    private static int[] trimmedRegion(String masked, String key, int contentStart, String parsedText) {
        int contentEnd = masked.indexOf('<', contentStart);
        if (contentEnd < 0 || !unescape(masked.substring(contentStart, contentEnd)).equals(parsedText)) {
            throw new IllegalStateException("Could not resolve the value of " + key + " in the template");
        }
        int start = contentStart;
        int end = contentEnd;
        while (start < end && Character.isWhitespace(masked.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(masked.charAt(end - 1))) {
            end--;
        }
        if (start == end) {
            throw new IllegalArgumentException("Template overlay path " + key + " addresses an empty element");
        }
        return new int[] {start, end};
    }

    private static XMLStreamReader newReader(String xml) throws XMLStreamException {
        // The JDK's own implementation, whatever StAX provider is on the classpath: its START_ELEMENT location is
        // the offset just after the start tag, which locateLeafRegions relies on (and verifies).
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        return factory.createXMLStreamReader(new StringReader(xml));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
