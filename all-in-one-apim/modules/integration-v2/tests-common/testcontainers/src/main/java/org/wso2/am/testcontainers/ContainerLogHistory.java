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
 */

package org.wso2.am.testcontainers;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Retained Log4j2 file history, ordered by the appender's date and numeric index, followed by the active file.
 * Current-file accessors remain separate. History is scoped to the live Carbon JVM, not the container's lifetime.
 */
public final class ContainerLogHistory {
    private static final Pattern TIMESTAMP = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}");
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    private static final Pattern CLOCK = Pattern.compile("\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}");
    private static final String START = "Starting WSO2 Carbon";
    private static final String SERVER_LOG = "wso2carbon.log";

    private static final ConcurrentMap<String, Long> JVM_STARTS = new ConcurrentHashMap<>();
    private static final int CACHE_LIMIT = 64 * 1024 * 1024;
    private static final java.util.Map<String, byte[]> BYTE_CACHE = new java.util.LinkedHashMap<>(16, 0.75f, true);
    private static long cachedBytes;

    /** Container transport failures can be polled without accepting incomplete log evidence. */
    public static final class ReadUnavailableException extends IllegalStateException {
        public ReadUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private ContainerLogHistory() {
    }

    /** Reads only the named appender and its Carbon boot boundary, on the specified component. */
    public static Snapshot read(GenericContainer<?> container, String serverHome, String fileName) {
        if (!fileName.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid log file name: " + fileName);
        }
        try {
            String config;
            try {
                config = container.copyFileFromContainer(serverHome + "/repository/conf/log4j2.properties",
                        stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception unavailable) {
                throw new ReadUnavailableException("Could not copy log4j2.properties", unavailable);
            }
            Properties properties = new Properties();
            properties.load(new StringReader(config));
            Layout layout = layout(properties, fileName);
            Layout carbon = layout(properties, SERVER_LOG);
            // Open all matching files before reading any content. Renames cannot invalidate an open descriptor.
            java.util.Map<String, byte[]> cached = cachedFiles(container.getContainerId());
            StringBuilder hints = new StringBuilder();
            for (var entry : cached.entrySet()) {
                hints.append(entry.getKey()).append(' ').append(entry.getValue().length).append(' ')
                        .append(hexDigest(entry.getValue())).append('\n');
            }
            Container.ExecResult result = exec(container, "bash", "-c", captureScript(), "log-history",
                    serverHome + "/repository/logs", layout.current, layout.shellGlob(), carbon.current, carbon.shellGlob(),
                    hints.toString());
            if (result.getExitCode() != 0) {
                throw new IOException("Log snapshot failed: " + result.getStderr());
            }
            String[] rows = result.getStdout().split("\\n");
            String[] process = rows[0].split("\\t");
            if (process.length != 3 || process[0].equals("none")) {
                throw new IOException("No live Carbon JVM for log history");
            }
            java.util.Set<String> capturedNames = new java.util.HashSet<>();
            for (int i = 1; i < rows.length; i++) {
                capturedNames.add(rows[i].split("\\t", -1)[0]);
            }
            List<File> files = new ArrayList<>();
            java.util.Map<String, byte[]> captured = new java.util.HashMap<>();
            for (int i = 1; i < rows.length; i++) {
                String[] fields = rows[i].split("\\t", -1);
                if (fields.length != 6) {
                    throw new IOException("Incomplete log snapshot record");
                }
                if (!layout.accepts(fields[0]) && !carbon.accepts(fields[0])) {
                    continue;
                }
                // Log4j keeps the source until compression closes; prefer it over an unfinished gzip stream.
                if (fields[0].endsWith(".gz")
                        && capturedNames.contains(fields[0].substring(0, fields[0].length() - 3))) {
                    continue;
                }
                byte[] bytes = mergeCaptured(cached.get(fields[1]), Integer.parseInt(fields[4]),
                        Base64.getDecoder().decode(fields[5]), Integer.parseInt(fields[3]));
                files.add(decode(fields[0], fields[1], bytes, Long.parseLong(fields[2])));
                captured.put(fields[1], bytes);
            }
            retainBytes(container.getContainerId(), captured);
            String identity = container.getContainerId() + ":" + process[0];
            ZoneOffset zone = ZoneOffset.of(process[1]);
            String carbonText = join(carbon, files);
            Snapshot boot = snapshot(identity, jvmStart(container, identity, process[0]), zone, SERVER_LOG, carbonText, carbonText);
            if (boot.bootFound()) {
                String bootDay = java.time.Instant.ofEpochMilli(timestamp(boot.text(), zone)).atOffset(zone)
                        .toLocalDate().toString();
                if (hasLaterUnsafePeriod(unsafePeriods(carbon, files), bootDay)) {
                    throw new IllegalStateException("Retention may have discarded Carbon events after the current boot");
                }
            }
            String text = SERVER_LOG.equals(fileName) ? boot.text()
                    : boot.bootFound() ? afterBoot(layout, files, timestamp(boot.text(), zone), zone) : "";
            List<File> retained = ordered(layout, files);
            java.util.Set<String> identities = retained.stream().map(File::identity)
                    .collect(java.util.stream.Collectors.toSet());
            String active = retained.stream().filter(f -> layout.current.equals(f.name())).map(File::identity)
                    .findFirst().orElse("");
            String period = boundaryPeriod(layout, retained, zone, Long.parseLong(process[2]));
            return new Snapshot(identity + ":" + fileName, text, boot.bootFound(), active, identities,
                    period, unsafePeriods(layout, retained));
        } catch (ReadUnavailableException e) {
            throw e;
        } catch (IOException e) {
            throw new ReadUnavailableException("Could not capture " + fileName + " history", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read " + fileName + " history: " + e.getMessage(), e);
        }
    }

    /** Cache only exact bytes. Every reuse requires the live inode's prefix to pass SHA-256 inside the container. */
    private static synchronized java.util.Map<String, byte[]> cachedFiles(String container) {
        java.util.Map<String, byte[]> files = new java.util.HashMap<>();
        String prefix = container + ":";
        BYTE_CACHE.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                files.put(key.substring(prefix.length()), value);
            }
        });
        return files;
    }

    private static synchronized void retainBytes(String container, java.util.Map<String, byte[]> files) {
        files.forEach((id, bytes) -> {
            if (bytes.length <= CACHE_LIMIT) {
                byte[] old = BYTE_CACHE.put(container + ":" + id, bytes);
                cachedBytes += bytes.length - (old == null ? 0 : old.length);
            }
        });
        var entries = BYTE_CACHE.entrySet().iterator();
        while (cachedBytes > CACHE_LIMIT && entries.hasNext()) {
            cachedBytes -= entries.next().getValue().length;
            entries.remove();
        }
    }

    static byte[] mergeCaptured(byte[] prefix, int offset, byte[] suffix, int size) throws IOException {
        if (offset < 0 || size < 0 || (long) offset + suffix.length != size
                || (offset > 0 && (prefix == null || prefix.length != offset))) {
            throw new IOException("Incomplete incremental log capture");
        }
        if (offset == 0) {
            return suffix;
        }
        if (suffix.length == 0) {
            return prefix;
        }
        byte[] bytes = java.util.Arrays.copyOf(prefix, size);
        System.arraycopy(suffix, 0, bytes, offset, suffix.length);
        return bytes;
    }

    private static String hexDigest(byte[] bytes) {
        try {
            StringBuilder hex = new StringBuilder(64);
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) {
                hex.append(Character.forDigit((value >>> 4) & 15, 16));
                hex.append(Character.forDigit(value & 15, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Container.ExecResult exec(GenericContainer<?> container, String... command) {
        try {
            return container.execInContainer(command);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ReadUnavailableException("Interrupted capturing log history", interrupted);
        } catch (Exception unavailable) {
            throw new ReadUnavailableException("Container log transport unavailable", unavailable);
        }
    }

    /** HotSpot records this epoch once at VM creation; a later wall-clock correction cannot move it. */
    private static long jvmStart(GenericContainer<?> container, String identity, String process) {
        return JVM_STARTS.computeIfAbsent(identity, ignored -> {
            String pid = process.substring(0, process.indexOf(':'));
            Container.ExecResult result = exec(container, "jcmd", pid, "PerfCounter.print");
            if (result.getExitCode() != 0) {
                throw new ReadUnavailableException("Cannot read JVM creation counter: " + result.getStderr(), null);
            }
            return creationTime(result.getStdout());
        });
    }

    static long creationTime(String counters) {
        Matcher time = Pattern.compile("(?m)^sun\\.rt\\.createVmBeginTime=(\\d+)\\r?$").matcher(counters);
        if (!time.find() || Long.parseLong(time.group(1)) <= 0) {
            throw new IllegalStateException("JVM has no positive sun.rt.createVmBeginTime counter; "
                    + "cannot establish its log boot boundary");
        }
        return Long.parseLong(time.group(1));
    }

    /* Bash descriptors pin archived inodes even when Log4j renames its numeric window during the read. */
    static String captureScript() {
        return String.join("\n",
                "set -euo pipefail",
                "process=none",
                "for p in /proc/[0-9]*; do",
                "  [ \"$(cat \"$p/comm\" 2>/dev/null || true)\" = java ] || continue",
                "  if grep -azq 'org.wso2.carbon.bootstrap.Bootstrap' \"$p/cmdline\" 2>/dev/null; then",
                "    ticks=$(awk '{print $22}' \"$p/stat\")",
                "    wall_ms=$(date +%s%3N)",
                "    process=\"${p##*/}:$ticks\"",
                "    break",
                "  fi",
                "done",
                "[ \"$process\" != none ] || { echo 'No live Carbon JVM' >&2; exit 1; }",
                "printf '%s\\t%s\\t%s\\n' \"$process\" \"$(date +%z)\" \"$wall_ms\"",
                "cd \"$1\"",
                "shopt -s nullglob extglob",
                "declare -A cached_size=() cached_hash=()",
                "while read -r id size hash; do",
                "  [ -n \"$id\" ] || continue",
                "  cached_size[$id]=$size; cached_hash[$id]=$hash",
                "done <<< \"${6:-}\"",
                "for capture in 1 2 3; do",
                "declare -A seen=()",
                "names=(); ids=(); fds=(); sizes=(); times=()",
                "stable=true",
                "# Active files are pinned first: a later roll may also expose their inode as an archive.",
                "for f in \"$2\" \"$4\"; do",
                "  [ -f \"$f\" ] || continue",
                "    if ! { exec {fd}<\"$f\"; } 2>/dev/null; then stable=false; continue; fi",
                "    id=$(stat -Lc '%d:%i' \"/proc/$$/fd/$fd\")",
                "    if [ \"${seen[$id]:-}\" = 1 ]; then exec {fd}<&-; continue; fi",
                "    names+=(\"$f\"); ids+=(\"$id\"); fds+=(\"$fd\")",
                "    sizes+=(\"$(stat -Lc '%s' \"/proc/$$/fd/$fd\")\")",
                "    times+=(\"$(stat -Lc '%Y' \"/proc/$$/fd/$fd\")\")",
                "    seen[$id]=1",
                "done",
                "for f in $3 $5; do",
                "  [ -f \"$f\" ] || continue",
                "  if ! { exec {fd}<\"$f\"; } 2>/dev/null; then stable=false; continue; fi",
                "  id=$(stat -Lc '%d:%i' \"/proc/$$/fd/$fd\")",
                "  if [ \"${seen[$id]:-}\" = 1 ]; then exec {fd}<&-; continue; fi",
                "  names+=(\"$f\"); ids+=(\"$id\"); fds+=(\"$fd\")",
                "  sizes+=(\"$(stat -Lc '%s' \"/proc/$$/fd/$fd\")\")",
                "  times+=(\"$(stat -Lc '%Y' \"/proc/$$/fd/$fd\")\")",
                "  seen[$id]=1",
                "done",
                "for i in \"${!fds[@]}\"; do",
                "  actual=$(stat -Lc '%d:%i' \"${names[$i]}\" 2>/dev/null || true)",
                "  [ \"$actual\" = \"${ids[$i]}\" ] || stable=false",
                "done",
                "if [ \"$stable\" = false ]; then",
                "  for fd in \"${fds[@]}\"; do exec {fd}<&-; done",
                "  continue",
                "fi",
                "# Once pinned and validated, a subsequent rollover cannot change these bytes or their order.",
                "for i in \"${!fds[@]}\"; do",
                "  offset=0; previous=${cached_size[${ids[$i]}]:-0}",
                "  if [ \"$previous\" -gt 0 ] && [ \"$previous\" -le \"${sizes[$i]}\" ]; then",
                "    hash=$(head -c \"$previous\" \"/proc/$$/fd/${fds[$i]}\" | sha256sum)",
                "    [ \"${hash%% *}\" != \"${cached_hash[${ids[$i]}]}\" ] || offset=$previous",
                "  fi",
                "  printf '%s\\t%s\\t%s\\t%s\\t%s\\t' \"${names[$i]}\" \"${ids[$i]}\" \"${times[$i]}\" \"${sizes[$i]}\" \"$offset\"",
                "  dd if=\"/proc/$$/fd/${fds[$i]}\" bs=64K iflag=skip_bytes,count_bytes skip=\"$offset\" count=\"$((sizes[$i] - offset))\" status=none | base64 -w0",
                "  printf '\\n'",
                "done",
                "[ -r \"$p/stat\" ] && [ \"$(awk '{print $22}' \"$p/stat\")\" = \"$ticks\" ] || exit 1",
                "exit 0",
                "done",
                "echo 'Log archive window changed during all three descriptor captures' >&2",
                "exit 1", "");
    }

    static File decode(String name, String identity, byte[] bytes) throws IOException {
        return decode(name, identity, bytes, 0);
    }

    private static File decode(String name, String identity, byte[] bytes, long modified) throws IOException {
        if (name.endsWith(".gz")) {
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = gzip.readAllBytes();
            }
        }
        return new File(name, identity, new String(bytes, StandardCharsets.UTF_8), modified);
    }

    /** One file in a retained appender window. Identity is its filesystem device/inode. */
    public static final class File {
        private final String name;
        private final String identity;
        private final String text;
        private final long modified;

        public File(String name, String identity, String text) {
            this(name, identity, text, 0);
        }

        public File(String name, String identity, String text, long modified) {
            this.modified = modified;
            this.name = name;
            this.identity = identity;
            this.text = text;
        }

        public String name() {
            return name;
        }

        public String identity() {
            return identity;
        }

        public String text() {
            return text;
        }

    }

    static Layout layout(Properties properties, String name) {
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("appender.") && key.endsWith(".fileName")
                    && basename(properties.getProperty(key)).equals(name)) {
                String prefix = key.substring(0, key.length() - ".fileName".length());
                String pattern = properties.getProperty(prefix + ".filePattern");
                if (pattern == null) {
                    throw new IllegalArgumentException("No rollover pattern for " + name);
                }
                if (!"max".equals(properties.getProperty(prefix + ".strategy.fileIndex", "max"))) {
                    throw new IllegalArgumentException("Unsupported archive index order for " + name);
                }
                String format = properties.getProperty(prefix + ".layout.pattern", "");
                boolean clockOnly = format.contains("%d{HH:mm:ss,SSS}") || format.contains("%d{HH:mm:ss.SSS}");
                boolean dated = Pattern.compile("%d(?![A-Za-z{])").matcher(format).find()
                        || format.contains("%d{DEFAULT}") || format.contains("%d{ISO8601}")
                        || format.contains("%d{yyyy-MM-dd HH:mm:ss,SSS}")
                        || format.contains("%d{yyyy-MM-dd'T'HH:mm:ss.SSS}");
                if (!clockOnly && !dated) {
                    throw new IllegalArgumentException("Log history requires a supported timestamp layout: " + name);
                }
                return new Layout(name, basename(pattern), clockOnly,
                        Integer.parseInt(properties.getProperty(prefix + ".strategy.max", "7")));
            }
        }
        throw new IllegalArgumentException("No file appender for " + name);
    }

    private static String basename(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Matches the shipped date/index/gzip patterns; unsupported patterns fail instead of guessing their order. */
    static final class Layout {
        private final String current;
        private final Pattern archives;
        private final boolean date;
        private final boolean index;
        private final String glob;
        private final boolean clockOnly;
        private final int maxArchives;

        Layout(String current, String pattern) {
            this(current, pattern, false);
        }

        Layout(String current, String pattern, boolean clockOnly) {
            this(current, pattern, clockOnly, 7);
        }

        Layout(String current, String pattern, boolean clockOnly, int maxArchives) {
            if (maxArchives <= 0) {
                throw new IllegalArgumentException("Invalid archive window: " + maxArchives);
            }
            this.maxArchives = maxArchives;
            this.clockOnly = clockOnly;
            if (clockOnly && !pattern.contains("%d{MM-dd-yyyy}")) {
                throw new IllegalArgumentException("Clock-only log requires dated archives: " + pattern);
            }
            this.current = current;
            date = pattern.contains("%d{MM-dd-yyyy}");
            index = pattern.contains("%i");
            String remainder = pattern.replace("%d{MM-dd-yyyy}", "").replace("%i", "");
            if (remainder.contains("%") || !remainder.matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalArgumentException("Unsupported rollover pattern: " + pattern);
            }
            String archivePattern = pattern.endsWith(".gz") ? pattern.substring(0, pattern.length() - 3) : pattern;
            String regex = Pattern.quote(archivePattern).replace("%d{MM-dd-yyyy}", "\\E(?<date>\\d{2}-\\d{2}-\\d{4})\\Q")
                    .replace("%i", "\\E(?<index>\\d+)\\Q");
            archives = Pattern.compile(regex + (pattern.endsWith(".gz") ? "(?:\\.gz)?" : ""));
            // The source of an in-progress gzip action shares the archive's date/index position.
            glob = pattern.replace("%d{MM-dd-yyyy}", "[0-9][0-9]-[0-9][0-9]-[0-9][0-9][0-9][0-9]")
                    .replace("%i", "[0-9]*").replace(".gz", "?(.gz)");
        }

        String shellGlob() {
            return glob;
        }

        boolean accepts(String name) {
            return current.equals(name) || archives.matcher(name).matches();
        }

        String order(String name) {
            if (current.equals(name)) {
                return "~";
            }
            Matcher match = archives.matcher(name);
            if (!match.matches()) {
                throw new IllegalArgumentException("Not an archive: " + name);
            }
            String day = date ? LocalDate.parse(match.group("date"), DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                    .toString() : "";
            return day + ":" + (index ? String.format("%010d", Long.parseLong(match.group("index"))) : "");
        }
    }

    private static List<File> ordered(Layout layout, List<File> files) {
        List<File> candidates = files.stream().filter(f -> layout.accepts(f.name()))
                .sorted(Comparator.comparing((File f) -> layout.order(f.name()))
                        .thenComparing(f -> f.name().endsWith(".gz")))
                .collect(java.util.stream.Collectors.toList());
        List<File> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.Set<String> positions = new java.util.HashSet<>();
        for (File file : candidates) {
            if (seen.add(file.identity()) && positions.add(layout.order(file.name()))) {
                result.add(file);
            }
        }
        return result;
    }

    /** A full numeric window, or a date-only archive, cannot prove that no older segment was overwritten. */
    static boolean completeWindow(Layout layout, List<File> files) {
        return unsafePeriods(layout, files).isEmpty();
    }

    private static java.util.Set<String> unsafePeriods(Layout layout, List<File> files) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        java.util.Set<String> unsafe = new java.util.HashSet<>();
        for (File file : ordered(layout, files)) {
            if (!layout.current.equals(file.name())) {
                String period = layout.date ? layout.order(file.name()).substring(0, 10) : "";
                if (!layout.index || counts.merge(period, 1, Integer::sum) >= layout.maxArchives) {
                    unsafe.add(period);
                }
            }
        }
        return unsafe;
    }

    /** An empty new active file does not move a mark past the last record in yesterday's archive. */
    static String boundaryPeriod(Layout layout, List<File> files, ZoneOffset zone, long capturedMillis) {
        if (!layout.date) {
            return "";
        }
        List<File> retained = ordered(layout, files);
        File boundary = retained.stream().filter(f -> !f.text().isEmpty()).reduce((first, last) -> last)
                .orElse(retained.isEmpty() ? null : retained.get(retained.size() - 1));
        if (boundary == null) {
            return java.time.Instant.ofEpochMilli(capturedMillis).atOffset(zone).toLocalDate().toString();
        }
        return layout.current.equals(boundary.name())
                ? java.time.Instant.ofEpochSecond(boundary.modified).atOffset(zone).toLocalDate().toString()
                : layout.order(boundary.name()).substring(0, 10);
    }

    static boolean hasLaterUnsafePeriod(java.util.Set<String> unsafe, String period) {
        return unsafe.stream().anyMatch(value -> !value.isEmpty() && value.compareTo(period) > 0);
    }

    static String join(Layout layout, List<File> files) {
        StringBuilder text = new StringBuilder();
        for (File file : ordered(layout, files)) {
            text.append(file.text());
        }
        return text.toString();
    }

    /** Clock-only layouts use the archive date, or the active file's modification date in the component's zone. */
    static String afterBoot(Layout layout, List<File> files, long boot, ZoneOffset zone) {
        StringBuilder text = new StringBuilder();
        boolean include = false;
        for (File file : ordered(layout, files)) {
            LocalDate day = null;
            if (layout.clockOnly) {
                if (layout.current.equals(file.name())) {
                    day = java.time.Instant.ofEpochSecond(file.modified).atOffset(zone).toLocalDate();
                } else {
                    Matcher archive = layout.archives.matcher(file.name());
                    if (!archive.matches()) {
                        throw new IllegalArgumentException("Not a dated archive: " + file.name());
                    }
                    day = LocalDate.parse(archive.group("date"), DateTimeFormatter.ofPattern("MM-dd-yyyy"));
                }
            }
            for (String row : file.text().split("(?<=\\n)", -1)) {
                Long time;
                if (day == null) {
                    time = timestamp(row, zone);
                } else {
                    Matcher clock = CLOCK.matcher(row);
                    time = clock.find() ? LocalDateTime.parse(day + " " + clock.group().replace('.', ','), LOG_TIME)
                            .toInstant(zone).toEpochMilli() : null;
                }
                if (time != null) {
                    include = time >= boot;
                }
                if (include) {
                    text.append(row);
                }
            }
        }
        return text.toString();
    }

    /** Restricts carbon to this process's last boot, and other appenders to timestamps from that boot onward. */
    static Snapshot snapshot(String process, long processStartMillis, ZoneOffset zone, String fileName,
                             String carbon, String content) {
        int start = carbon.lastIndexOf(START);
        int line = start < 0 ? -1 : carbon.lastIndexOf('\n', start) + 1;
        Long boot = line < 0 ? null : timestamp(carbon.substring(line, start), zone);
        if (boot == null || boot < processStartMillis) {
            return new Snapshot(process + ":" + fileName, "", false);
        }
        if (SERVER_LOG.equals(fileName)) {
            content = carbon.substring(line);
        } else {
            content = afterBoot(new Layout(fileName, fileName),
                    List.of(new File(fileName, fileName, content)), boot, zone);
        }
        return new Snapshot(process + ":" + fileName, content, true);
    }

    private static Long timestamp(String line, ZoneOffset zone) {
        Matcher time = TIMESTAMP.matcher(line);
        if (!time.find()) {
            return null;
        }
        return LocalDateTime.parse(time.group().replace('.', ',').replace('T', ' '), LOG_TIME)
                .toInstant(zone).toEpochMilli();
    }

    /** A boot-scoped immutable observation. A missing current-boot marker supplies no readiness evidence. */
    public static final class Snapshot {
        private final String identity;
        private final String text;
        private final boolean bootFound;
        private final String activeFile;
        private final java.util.Set<String> retainedFiles;
        private final String period;
        private final java.util.Set<String> unsafePeriods;

        public Snapshot(String identity, String text, boolean bootFound) {
            this(identity, text, bootFound, "", java.util.Set.of(), true);
        }

        public Snapshot(String identity, String text, boolean bootFound, String activeFile,
                        java.util.Set<String> retainedFiles, boolean completeWindow) {
            this(identity, text, bootFound, activeFile, retainedFiles, "",
                    completeWindow ? java.util.Set.of() : java.util.Set.of(""));
        }

        public Snapshot(String identity, String text, boolean bootFound, String activeFile,
                        java.util.Set<String> retainedFiles, String period, java.util.Set<String> unsafePeriods) {
            this.activeFile = activeFile;
            this.retainedFiles = java.util.Set.copyOf(retainedFiles);
            this.period = period;
            this.unsafePeriods = java.util.Set.copyOf(unsafePeriods);
            this.identity = identity;
            this.text = text;
            this.bootFound = bootFound;
        }

        public String identity() {
            return identity;
        }

        public String text() {
            return text;
        }

        public boolean bootFound() {
            return bootFound;
        }

        public Mark mark() {
            if (!bootFound) {
                throw new IllegalStateException("Current boot has no 'Starting WSO2 Carbon' marker");
            }
            return new Mark(identity, text.length(), digest(text), text.substring(Math.max(0, text.length() - 4096)),
                    !text.isEmpty() && !text.endsWith("\n"), activeFile, period);
        }

        /** Reads strictly after a mark, including a mark whose file is now archived or compressed. */
        public String since(Mark mark) {
            if (bootFound && laterBootOfSameLog(mark.identity())) {
                // The mark predates this Carbon process, so all of this boot's history follows it.
                return text;
            }
            if (!bootFound || !identity.equals(mark.identity())) {
                throw new IllegalStateException("Log mark belongs to a different Carbon boot or log file");
            }
            if (hasLaterUnsafePeriod(unsafePeriods, mark.period)) {
                throw new IllegalStateException("Retention may have discarded events in a later rollover period");
            }
            boolean unsafeHere = unsafePeriods.contains(mark.period) || unsafePeriods.contains("");
            boolean prefixMatches = text.length() >= mark.offset()
                    && digest(text.substring(0, mark.offset())).equals(mark.digest());
            if (unsafeHere && (!retainedFiles.contains(mark.file) || !prefixMatches)) {
                throw new IllegalStateException("Retention may have discarded events after the log mark");
            }
            if (prefixMatches) {
                return afterMarkedLine(mark, text.substring(mark.offset()));
            }
            int anchor = mark.anchor().isEmpty() ? -1 : text.indexOf(mark.anchor());
            if (anchor < 0 || text.indexOf(mark.anchor(), anchor + 1) >= 0) {
                throw new IllegalStateException("Retained log history no longer contains an unambiguous log mark");
            }
            return afterMarkedLine(mark, text.substring(anchor + mark.anchor().length()));
        }

        /**
         * Identities are {@code <container>:<pid>:<start ticks>:<file>}. A mark from another Carbon process in the
         * same container and file was taken before this process started (a restart), because snapshots are read
         * after the marks they are compared with.
         */
        private boolean laterBootOfSameLog(String markIdentity) {
            String[] current = identity.split(":");
            String[] marked = markIdentity.split(":");
            return current.length == 4 && marked.length == 4
                    && current[0].equals(marked[0]) && current[3].equals(marked[3])
                    && !(current[1].equals(marked[1]) && current[2].equals(marked[2]));
        }
    }

    /** Content identity survives numeric archive renaming, compression and expiry of older prefixes. */
    public static final class Mark {
        private final String identity;
        private final int offset;
        private final String digest;
        private final String anchor;
        private final boolean partialLine;
        private final String file;
        private final String period;

        public Mark(String identity, int offset, String digest, String anchor, boolean partialLine, String file,
                    String period) {
            this.period = period;
            this.file = file;
            this.partialLine = partialLine;
            this.identity = identity;
            this.offset = offset;
            this.digest = digest;
            this.anchor = anchor;
        }

        public String identity() {
            return identity;
        }

        public int offset() {
            return offset;
        }

        public String digest() {
            return digest;
        }

        public String anchor() {
            return anchor;
        }

    }

    /** A line already being written at the mark cannot become a new assertion event. */
    private static String afterMarkedLine(Mark mark, String appended) {
        if (!mark.partialLine) {
            return appended;
        }
        int end = appended.indexOf('\n');
        return end < 0 ? "" : appended.substring(end + 1);
    }

    private static String digest(String text) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
