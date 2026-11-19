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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/** Synthetic retained files exercise appender ordering, boot isolation and marks without a server. */
public class ContainerLogHistoryTest {
    private static final String START = "TID: [] [2026-10-22 23:59:50,000] INFO - Starting WSO2 Carbon...\n";
    private static final long PROCESS = Instant.parse("2026-10-22T23:59:49Z").toEpochMilli();
    private static final ContainerLogHistory.Layout CARBON = new ContainerLogHistory.Layout(
            "wso2carbon.log", "wso2carbon-%d{MM-dd-yyyy}-%i.log");

    @Test
    public void incrementalCaptureKeepsExactBytesAndRejectsAnUnavailablePrefix() throws Exception {
        byte[] bytes = "before \u20ac after\n".getBytes(StandardCharsets.UTF_8);
        byte[] prefix = java.util.Arrays.copyOf(bytes, 8);
        byte[] suffix = java.util.Arrays.copyOfRange(bytes, 8, bytes.length);
        Assert.assertEquals(ContainerLogHistory.mergeCaptured(prefix, 8, suffix, bytes.length), bytes);
        Assert.assertSame(ContainerLogHistory.mergeCaptured(bytes, bytes.length, new byte[0], bytes.length), bytes);
        Assert.assertEquals(ContainerLogHistory.mergeCaptured(null, 0, suffix, suffix.length), suffix);
        Assert.expectThrows(java.io.IOException.class,
                () -> ContainerLogHistory.mergeCaptured(null, 8, suffix, bytes.length));
        Assert.expectThrows(java.io.IOException.class,
                () -> ContainerLogHistory.mergeCaptured(prefix, 8, suffix, bytes.length + 1));
    }

    @Test
    public void recordedVmCreationTimeDefinesTheBootBoundary() {
        long created = ContainerLogHistory.creationTime("123:\nsun.rt.createVmBeginTime=" + PROCESS + "\n");
        Assert.assertEquals(created, PROCESS);
        Assert.assertTrue(ContainerLogHistory.snapshot("pid:ticks", created, ZoneOffset.UTC,
                "wso2carbon.log", START, START).bootFound());
        Assert.assertFalse(ContainerLogHistory.snapshot("new-pid:ticks", created + 2000, ZoneOffset.UTC,
                "wso2carbon.log", START, START).bootFound());
        Assert.expectThrows(IllegalStateException.class, () -> ContainerLogHistory.creationTime("no counter"));
    }

    @Test
    public void midnightMidBootKeepsTheStartAndTenantEvidence() throws Exception {
        var snapshot = carbon(List.of(file("wso2carbon-10-22-2026-1.log", START + "Loaded tenant tenant1.com in 5 ms\n"),
                file("wso2carbon.log", "Synapse Artifacts deployed Successfully in the Gateway\n")));
        Assert.assertTrue(snapshot.bootFound());
        Assert.assertTrue(snapshot.text().contains("Loaded tenant tenant1.com"));
        Assert.assertTrue(snapshot.text().endsWith("Synapse Artifacts deployed Successfully in the Gateway\n"));
    }

    @Test
    public void sizeArchivesUseNumericRatherThanLexicalOrder() throws Exception {
        String text = ContainerLogHistory.join(CARBON, List.of(
                file("wso2carbon-10-22-2026-10.log", "ten\n"), file("wso2carbon.log", "current\n"),
                file("wso2carbon-10-22-2026-2.log", "two\n"), file("wso2carbon-10-22-2026-1.log", START)));
        Assert.assertEquals(text, START + "two\nten\ncurrent\n");
    }

    @Test
    public void combinedRolloverOrdersAcrossYearBoundary() throws Exception {
        Assert.assertEquals(ContainerLogHistory.join(CARBON, List.of(
                file("wso2carbon-01-01-2027-1.log", "new year\n"),
                file("wso2carbon-12-31-2026-11.log", "eleven\n"),
                file("wso2carbon-12-31-2026-2.log", "two\n"), file("wso2carbon.log", "now\n"))),
                "two\neleven\nnew year\nnow\n");
    }

    @Test
    public void restartDiscardsAllOldBootEvidenceIncludingArchivedMarkers() throws Exception {
        String second = "[2026-10-23 00:01:00,000] INFO Starting WSO2 Carbon...\n";
        String all = ContainerLogHistory.join(CARBON, List.of(file("wso2carbon-10-22-2026-1.log",
                START + "Loaded tenant tenant1.com in 5 ms\n"), file("wso2carbon.log", second + "not ready\n")));
        var snapshot = ContainerLogHistory.snapshot("new-process", PROCESS + 60_000, ZoneOffset.UTC,
                "wso2carbon.log", all, all);
        Assert.assertTrue(snapshot.bootFound());
        Assert.assertFalse(snapshot.text().contains("Loaded tenant"));
        Assert.assertEquals(snapshot.text(), second + "not ready\n");
    }

    @Test
    public void newProcessCannotUseOldMarkerBeforeItsOwnMarkerAppears() {
        var snapshot = ContainerLogHistory.snapshot("new-process", PROCESS + 60_000, ZoneOffset.UTC,
                "wso2carbon.log", START + "old readiness\n", START + "old readiness\n");
        Assert.assertFalse(snapshot.bootFound());
        Assert.assertEquals(snapshot.text(), "");
        Assert.expectThrows(IllegalStateException.class, snapshot::mark);
    }

    @Test
    public void genuinelyMissingStartIsNotEvidence() {
        var snapshot = ContainerLogHistory.snapshot("process", PROCESS, ZoneOffset.UTC,
                "wso2carbon.log", "Loaded tenant tenant1.com in 5 ms\n", "ready\n");
        Assert.assertFalse(snapshot.bootFound());
        Assert.assertEquals(snapshot.text(), "");
        Assert.assertTrue(Assert.expectThrows(IllegalStateException.class, snapshot::mark).getMessage()
                .contains("no 'Starting WSO2 Carbon' marker"));
    }

    @Test
    public void markSurvivesRolloverAndNeverMatchesAnOldDeploy() throws Exception {
        var before = carbon(List.of(file("wso2carbon.log", START + "old artifact was added\n")));
        var mark = before.mark();
        var after = carbon(List.of(file("wso2carbon-10-22-2026-1.log", before.text()),
                file("wso2carbon.log", "new artifact was added\n")));
        Assert.assertEquals(after.since(mark), "new artifact was added\n");
        Assert.assertFalse(after.since(mark).contains("old artifact"));
    }

    @Test
    public void markBeforeRestartAcceptsTheWholeLaterBoot() {
        var mark = new ContainerLogHistory.Snapshot("container:100:5000:wso2carbon.log",
                START + "old artifact was added\n", true).mark();
        var restarted = new ContainerLogHistory.Snapshot("container:200:9000:wso2carbon.log",
                START + "new artifact was added\n", true);
        Assert.assertEquals(restarted.since(mark), START + "new artifact was added\n");
    }

    @Test
    public void markDoesNotCrossContainersOrFiles() {
        var mark = new ContainerLogHistory.Snapshot("container:100:5000:wso2carbon.log", START, true).mark();
        var otherContainer = new ContainerLogHistory.Snapshot("other:200:9000:wso2carbon.log", START, true);
        var otherFile = new ContainerLogHistory.Snapshot("container:200:9000:correlation.log", START, true);
        var noBoot = new ContainerLogHistory.Snapshot("container:200:9000:wso2carbon.log", "", false);
        for (var snapshot : List.of(otherContainer, otherFile, noBoot)) {
            Assert.assertTrue(Assert.expectThrows(IllegalStateException.class, () -> snapshot.since(mark))
                    .getMessage().contains("different Carbon boot"));
        }
    }

    @Test
    public void expiredOrAmbiguousMarksFailInsteadOfReadingTheWholeFile() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "marked boundary\n", true).mark();
        var expired = new ContainerLogHistory.Snapshot("boot:file", "new records only\n", true);
        Assert.expectThrows(IllegalStateException.class, () -> expired.since(mark));
        var ambiguous = new ContainerLogHistory.Snapshot("boot:file", "prefix\nmarked boundary\nmarked boundary\n", true);
        Assert.expectThrows(IllegalStateException.class, () -> ambiguous.since(mark));
    }

    @Test
    public void retainedMarkStillWorksWhenAnOlderPrefixExpires() {
        String prefix = "x".repeat(5000);
        var mark = new ContainerLogHistory.Snapshot("boot:file", prefix + "boundary\n", true).mark();
        var expired = new ContainerLogHistory.Snapshot("boot:file", prefix.substring(3000) + "boundary\nnew\n", true);
        // The retained tail is shorter than the anchor: expiry is a clear failure, never a false match.
        Assert.expectThrows(IllegalStateException.class, () -> expired.since(mark));
        var retained = new ContainerLogHistory.Snapshot("boot:file", prefix.substring(500) + "boundary\nnew\n", true);
        Assert.assertEquals(retained.since(mark), "new\n");
    }

    @Test
    public void gzipArchivesAndMarkAreReadAsOriginalText() throws Exception {
        Path directory = Files.createTempDirectory("log-history-gzip-");
        try {
            Path gz = directory.resolve("correlation-10-22-2026-1.log.gz");
            String previous = "2026-10-22 23:59:51,000|old\n";
            try (var output = new GZIPOutputStream(Files.newOutputStream(gz))) {
                output.write(previous.getBytes(StandardCharsets.UTF_8));
            }
            Path current = directory.resolve("correlation.log");
            Files.writeString(current, "2026-10-23 00:00:01,000|new\n");
            var layout = new ContainerLogHistory.Layout("correlation.log", "correlation-%d{MM-dd-yyyy}-%i.log.gz");
            String text = ContainerLogHistory.join(layout, List.of(
                    ContainerLogHistory.decode(gz.getFileName().toString(), "archive", Files.readAllBytes(gz)),
                    ContainerLogHistory.decode(current.getFileName().toString(), "active", Files.readAllBytes(current))));
            var before = ContainerLogHistory.snapshot("process", PROCESS, ZoneOffset.UTC, "correlation.log", START, previous);
            var after = ContainerLogHistory.snapshot("process", PROCESS, ZoneOffset.UTC, "correlation.log", START, text);
            Assert.assertEquals(after.since(before.mark()), "2026-10-23 00:00:01,000|new\n");
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.toList()) {
                    Files.delete(path);
                }
            }
            Files.delete(directory);
        }
    }

    @Test
    public void auditHistoryExcludesPreviousBootAndKeepsContinuationLines() {
        String content = "[2026-10-22 23:58:00,000] old\nold continuation\n"
                + "[2026-10-22 23:59:51,000] new\nnew continuation\n";
        var snapshot = ContainerLogHistory.snapshot("process", PROCESS, ZoneOffset.UTC, "audit.log", START, content);
        Assert.assertEquals(snapshot.text(), "[2026-10-22 23:59:51,000] new\nnew continuation\n");
    }

    @Test
    public void sameInodeIsNotDuplicatedAndOtherAppendersAreExcluded() throws Exception {
        Assert.assertEquals(ContainerLogHistory.join(CARBON, List.of(
                new ContainerLogHistory.File("wso2carbon-10-22-2026-1.log", "same", START),
                new ContainerLogHistory.File("wso2carbon.log", "same", START), file("audit.log", "other\n"))), START);
    }

    @Test
    public void layoutComesFromTheNamedAppenderAndUnsupportedFormatsFail() {
        Properties config = new Properties();
        config.setProperty("appender.CARBON.fileName", "${sys:carbon.home}/repository/logs/wso2carbon.log");
        config.setProperty("appender.CARBON.filePattern", "${sys:carbon.home}/repository/logs/wso2carbon-%d{MM-dd-yyyy}-%i.log");
        config.setProperty("appender.CARBON.layout.pattern", "%d %m%n");
        Assert.assertTrue(ContainerLogHistory.layout(config, "wso2carbon.log").accepts("wso2carbon-10-22-2026-1.log"));
        Assert.assertFalse(ContainerLogHistory.layout(config, "wso2carbon.log").accepts("wso2carbon-trace-messages.log"));
        Assert.expectThrows(IllegalArgumentException.class, () -> ContainerLogHistory.layout(config, "unknown.log"));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> new ContainerLogHistory.Layout("x.log", "x-%d{unsupported}.log"));
        config.setProperty("appender.CARBON.layout.pattern", "%msg%n");
        Assert.expectThrows(IllegalArgumentException.class, () -> ContainerLogHistory.layout(config, "wso2carbon.log"));
    }

    @Test
    public void compressionSourceAndDestinationAreOneChronologicalSegment() {
        var layout = new ContainerLogHistory.Layout("correlation.log", "correlation-%d{MM-dd-yyyy}-%i.log.gz");
        Assert.assertEquals(ContainerLogHistory.join(layout, List.of(
                new ContainerLogHistory.File("correlation-10-22-2026-1.log.gz", "gzip", "old\n"),
                new ContainerLogHistory.File("correlation-10-22-2026-1.log", "source", "old\n"),
                new ContainerLogHistory.File("correlation.log", "active", "new\n"))), "old\nnew\n");
    }

    @Test
    public void bootTimestampUsesTheComponentsZone() {
        String local = "[2026-10-23 05:29:50,000] INFO Starting WSO2 Carbon...\n";
        var snapshot = ContainerLogHistory.snapshot("process", PROCESS, ZoneOffset.of("+0530"),
                "wso2carbon.log", local, local);
        Assert.assertTrue(snapshot.bootFound());
    }

    @Test
    public void clockOnlyMetricsKeepTheirMeaningAcrossMidnightAndRestart() {
        var layout = new ContainerLogHistory.Layout("apim_metrics.log", "apim_metrics-%d{MM-dd-yyyy}-%i.log", true);
        long modified = Instant.parse("2026-10-23T00:00:02Z").getEpochSecond();
        List<ContainerLogHistory.File> files = List.of(
                new ContainerLogHistory.File("apim_metrics-10-22-2026-1.log", "one",
                        "[23:59:00,000] old boot\n[23:59:51,000] this boot\n"),
                new ContainerLogHistory.File("apim_metrics.log", "active", "[00:00:01,000] after midnight\n", modified));
        long boot = Instant.parse("2026-10-22T23:59:50Z").toEpochMilli();
        Assert.assertEquals(ContainerLogHistory.afterBoot(layout, files, boot, ZoneOffset.UTC),
                "[23:59:51,000] this boot\n[00:00:01,000] after midnight\n");
        Assert.assertEquals(ContainerLogHistory.afterBoot(layout, files, boot + 20_000, ZoneOffset.UTC), "");
    }

    @Test
    public void clockOnlyLayoutsRequireAnUnambiguousArchiveDate() {
        Assert.expectThrows(IllegalArgumentException.class,
                () -> new ContainerLogHistory.Layout("time.log", "time-%i.log", true));
    }

    @Test
    public void archiveGlobSelectsRawAndGzipSourcesWithoutOtherAppenders() throws Exception {
        Path directory = Files.createTempDirectory("log-history-glob-");
        try {
            List<String> names = List.of("correlation-10-22-2026-1.log", "correlation-10-22-2026-1.log.gz",
                    "correlation-10-22-2026-12.log.gz", "correlation.log", "correlation-other.log.gz");
            for (String name : names) {
                Files.writeString(directory.resolve(name), "");
            }
            var layout = new ContainerLogHistory.Layout("correlation.log", "correlation-%d{MM-dd-yyyy}-%i.log.gz");
            Process process = new ProcessBuilder("bash", "-c", "shopt -s nullglob extglob; cd \"$1\"; "
                    + "for f in $2; do printf '%s\\n' \"$f\"; done", "archive-glob", directory.toString(),
                    layout.shellGlob()).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertEquals(process.waitFor(), 0);
            Assert.assertEquals(output.lines().sorted().toList(), names.subList(0, 3).stream().sorted().toList());
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.toList()) {
                    Files.delete(path);
                }
            }
            Files.delete(directory);
        }
    }

    @Test
    public void aLineStartedBeforeTheMarkCannotBecomeANewEventAfterRollover() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "old line prefix ", true).mark();
        var partial = new ContainerLogHistory.Snapshot("boot:file", "old line prefix old artifact was added", true);
        Assert.assertEquals(partial.since(mark), "");
        var complete = new ContainerLogHistory.Snapshot("boot:file",
                "old line prefix old artifact was added\nnew artifact was added\n", true);
        Assert.assertEquals(complete.since(mark), "new artifact was added\n");
    }

    @Test
    public void sameSecondOldBootMarkerIsBeforeTheLiveProcessBoundary() {
        var old = ContainerLogHistory.snapshot("new-process", PROCESS + 1001, ZoneOffset.UTC,
                "wso2carbon.log", START + "old ready\n", START + "old ready\n");
        Assert.assertFalse(old.bootFound());
        String newStart = START.replace("23:59:50,000", "23:59:50,002");
        Assert.assertTrue(ContainerLogHistory.snapshot("new-process", PROCESS + 1001, ZoneOffset.UTC,
                "wso2carbon.log", newStart, newStart).bootFound());
    }

    @Test
    public void emptyMarkCannotHideEventsDiscardedByRetention() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "", true, "old-inode", java.util.Set.of("old-inode"),
                true).mark();
        var lost = new ContainerLogHistory.Snapshot("boot:file", "remaining events\n", true, "new-inode",
                java.util.Set.of("new-inode"), false);
        Assert.assertTrue(Assert.expectThrows(IllegalStateException.class, () -> lost.since(mark))
                .getMessage().contains("Retention may have discarded events"));
    }

    @Test
    public void emptyMarkSurvivesRawRolloverWhenItsInodeIsRetained() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "", true, "old-inode", java.util.Set.of("old-inode"),
                false).mark();
        var retained = new ContainerLogHistory.Snapshot("boot:file", "new events\n", true, "new-inode",
                java.util.Set.of("old-inode", "new-inode"), false);
        Assert.assertEquals(retained.since(mark), "new events\n");
    }

    @Test
    public void emptyMarkSurvivesGzipRolloverWithinACompleteWindow() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "", true, "old-inode", java.util.Set.of("old-inode"),
                true).mark();
        var retained = new ContainerLogHistory.Snapshot("boot:file", "new events\n", true, "new-inode",
                java.util.Set.of("gzip-inode", "new-inode"), true);
        Assert.assertEquals(retained.since(mark), "new events\n");
    }

    @Test
    public void completeWindowRequiresAnUnfilledIndexWindowOrTheMarkedInode() throws Exception {
        var indexed = new ContainerLogHistory.Layout("api.log", "api-%d{MM-dd-yyyy}-%i.log", false, 2);
        Assert.assertTrue(ContainerLogHistory.completeWindow(indexed, List.of(file("api-10-23-2026-1.log", "a\n"))));
        Assert.assertFalse(ContainerLogHistory.completeWindow(indexed, List.of(file("api-10-23-2026-1.log", "a\n"),
                file("api-10-23-2026-2.log", "b\n"))));
        var audit = new ContainerLogHistory.Layout("audit.log", "audit-%d{MM-dd-yyyy}.log");
        Assert.assertFalse(ContainerLogHistory.completeWindow(audit, List.of(file("audit-10-23-2026.log", "a\n"))));
    }

    @Test
    public void aRetainedOldDayMarkCannotHideEventsLostFromANewerDay() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "before\n", true, "old",
                java.util.Set.of("old"), "2026-10-22", java.util.Set.of()).mark();
        var gap = new ContainerLogHistory.Snapshot("boot:file", "before\nremaining new day records\n", true, "new",
                java.util.Set.of("old", "new"), "2026-10-23", java.util.Set.of("2026-10-23"));
        Assert.assertTrue(Assert.expectThrows(IllegalStateException.class, () -> gap.since(mark))
                .getMessage().contains("later rollover period"));
    }

    @Test
    public void oldBootsFullWindowsDoNotBecomeCurrentPeriodGaps() {
        Assert.assertFalse(ContainerLogHistory.hasLaterUnsafePeriod(java.util.Set.of("2026-10-21"), "2026-10-22"));
        Assert.assertFalse(ContainerLogHistory.hasLaterUnsafePeriod(java.util.Set.of("2026-10-22"), "2026-10-22"));
        Assert.assertTrue(ContainerLogHistory.hasLaterUnsafePeriod(java.util.Set.of("2026-10-23"), "2026-10-22"));
    }

    @Test
    public void anEmptyNewDayFileDoesNotMoveTheMarkedRecordBoundaryIntoThatDay() {
        var layout = new ContainerLogHistory.Layout("api.log", "api-%d{MM-dd-yyyy}-%i.log");
        long nextDay = Instant.parse("2026-10-23T00:00:01Z").toEpochMilli();
        var archive = new ContainerLogHistory.File("api-10-22-2026-1.log", "old", "before\n");
        Assert.assertEquals(ContainerLogHistory.boundaryPeriod(layout, List.of(archive,
                new ContainerLogHistory.File("api.log", "new", "", nextDay / 1000)), ZoneOffset.UTC, nextDay),
                "2026-10-22");
        Assert.assertEquals(ContainerLogHistory.boundaryPeriod(layout, List.of(archive,
                new ContainerLogHistory.File("api.log", "new", "after\n", nextDay / 1000)), ZoneOffset.UTC, nextDay),
                "2026-10-23");
    }

    @Test
    public void aFullWindowRequiresPhysicalMarkIdentityEvenWithMatchingText() {
        var mark = new ContainerLogHistory.Snapshot("boot:file", "before\n", true, "old-inode",
                java.util.Set.of("old-inode"), false).mark();
        var lost = new ContainerLogHistory.Snapshot("boot:file", "before\nnew\n", true, "new-inode",
                java.util.Set.of("new-inode"), false);
        Assert.expectThrows(IllegalStateException.class, () -> lost.since(mark));
    }

    private static ContainerLogHistory.File file(String name, String text) throws Exception {
        return ContainerLogHistory.decode(name, name, text.getBytes(StandardCharsets.UTF_8));
    }

    private static ContainerLogHistory.Snapshot carbon(List<ContainerLogHistory.File> files) {
        String text = ContainerLogHistory.join(CARBON, files);
        return ContainerLogHistory.snapshot("process:wso2carbon.log", PROCESS, ZoneOffset.UTC, "wso2carbon.log", text, text);
    }
}
