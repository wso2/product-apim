/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.benchmarktest;

import static junit.framework.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.enums.OperatingSystems;

public class BenchmarkUtils extends APIMIntegrationBaseTest {

    private static final String APIM_HOME = System.getProperty("carbon.home");
    private static final String IN_FILE_PATH = APIM_HOME + "/repository/logs/correlation.log";
    private static final String OUT_FILE_PATH = "logs/benchmark-tests/"; // This dir is initially created by the maven-antrun-plugin
    private static final String SUPER_TENANT = "superTenant";
    public static String tenant;

    public static int extractCountsFromLog(String logFile, String testType, LocalTime startTime, String provider)
            throws InterruptedException, APIManagerIntegrationTestException {

        String tenantName;
        String correlationID = System.getProperty("testName");
        if (correlationID == null) {
            correlationID = "";
        } else {
            correlationID = correlationID.toLowerCase();
        }
        if (tenant != SUPER_TENANT) {
            tenantName = provider;
        } else {
            tenantName = tenant;
        }
        String logAttribute = null;
        File directory = new File(OUT_FILE_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
        if (testType == "http") {
            logAttribute = "http-in-request";
        } else {
            logAttribute = testType;
        }
        int noOfLinesExecuted = 0;
        int loop = 0;
        int previous;
        if (testType == "jdbc") {
            // Wait till all the correlation logs are printed
            do {
                loop++;
                previous = fetchNoOflines(startTime, logAttribute, correlationID);
                Thread.sleep(2000);
            } while (previous != fetchNoOflines(startTime, logAttribute, correlationID) && loop < 10);
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));
             // lines executed are logged in to a file starting with test method name,
             Writer writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(OUT_FILE_PATH + logFile + "_" + testType + "_" + tenantName + ".log"), "utf-8"))
        ) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
                // lines executed will be captured with log Attribute
                if (logLine.contains("|" + logAttribute + "|")) {
                    String regex = "(\\d{2}:\\d{2}:\\d{2})";
                    Matcher matcher = Pattern.compile(regex).matcher(logLine);
                    if (matcher.find()) {
                        LocalTime logtime = LocalTime.parse(matcher.group(1));
                        if (logtime.isAfter(startTime)) {
                            logLine.substring(logLine.indexOf("|" + logAttribute + "|") + 3, logLine.length());
                            if (logLine.contains("|" + correlationID + "|")) {
                                noOfLinesExecuted++;
                                writer.write(logLine);
                                writer.write(System.lineSeparator());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            String errorMsg = "Error while reading/writing benchmark test log files";
            throw new APIManagerIntegrationTestException(errorMsg, e);
        }
        return noOfLinesExecuted;
    }

    public static int fetchNoOflines(LocalTime startTime, String logAttribute, String correlationID) throws APIManagerIntegrationTestException {

        int noOfLinesExecuted = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(IN_FILE_PATH));) {
            String readLine;
            while ((readLine = bufferedReader.readLine()) != null) {
                String logLine = readLine.toLowerCase();
                // lines executed will be captured with log Attribute
                if (logLine.contains("|" + logAttribute + "|")) {
                    String regex = "(\\d{2}:\\d{2}:\\d{2})";
                    Matcher matcher = Pattern.compile(regex).matcher(logLine);
                    if (matcher.find()) {
                        LocalTime logtime = LocalTime.parse(matcher.group(1));
                        if (logtime.isAfter(startTime)) {
                            logLine.substring(logLine.indexOf("|" + logAttribute + "|") + 3, logLine.length());
                            if (logLine.contains("|" + correlationID + "|")) {
                                noOfLinesExecuted++;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            String errorMsg = "Error while reading correlation log files";
            throw new APIManagerIntegrationTestException(errorMsg, e);
        }
        return noOfLinesExecuted;
    }

    public static LocalTime getCurrentTimeStampAndSetCorrelationID(String testName) throws InterruptedException {

        Thread.sleep(1500);
        LocalDateTime ldt = LocalDateTime.now();
        LocalTime currentTime = LocalTime.parse(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH).format(ldt));
        Thread.sleep(1000);
        System.setProperty("testName", testName + "_" + currentTime);
        return currentTime;
    }

    public static void validateBenchmark(int benchmark, int actualCount, String testType) {

        boolean exceedsLimit = false;
        String message = "External API requests";
        if (testType == "jdbc") {
            message = "SQL Queries executed";
        }
        // Validate if No. of statements executed exceeds the benchmark
        if (actualCount > benchmark) {
            exceedsLimit = true;
        }
        assertFalse(
                "Exceeded the Benchmark value of " + message + "! Benchmark value is " + benchmark + " But " + actualCount + " were Executed",
                exceedsLimit);
    }

    public static String getSystemResourceLocation() {

        String resourceLocation = System.getProperty("framework.resource.location");
        if (System.getProperty("os.name").toLowerCase().contains(OperatingSystems.WINDOWS.name().toLowerCase())) {
            resourceLocation = resourceLocation.replace("/", "\\");
        } else {
            resourceLocation = resourceLocation.replace("/", "/");
        }
        return resourceLocation;
    }

    public static int getBenchmark(String testType, String scenario) throws IOException, ParseException {

        String benchmarkValue = null;
        String resourceLocation;
        String tenantName;
        if (tenant != SUPER_TENANT) {
            tenantName = "tenant";
        } else {
            tenantName = tenant;
        }
        resourceLocation = getSystemResourceLocation() + "benchmark-values" + File.separator + "benchmark-values-" + testType + "-" + tenantName + ".json";
        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new FileReader(resourceLocation));
        for (Object o : a) {
            JSONObject values = (JSONObject) o;
            benchmarkValue = (String) values.get(scenario);
        }
        return Integer.parseInt(benchmarkValue);
    }

    public static void writeResultsToFile(String fileName, String testName, int actual, int benchmark, String provider) throws IOException {

        String tenantName;
        if (tenant != SUPER_TENANT) {
            tenantName = provider;
        } else {
            tenantName = tenant;
        }
        String outputFile = OUT_FILE_PATH + "Results_" + fileName + ".log";
        File f = new File(outputFile);
        if (!f.exists()) {
            f.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
        String logLine = testName + "  :  " + tenantName + "  :  Actual count is " + actual + "   Benchmark is  " + benchmark;
        if (actual > benchmark) {
            logLine = logLine + "           ------------>>>>> EXCEEDS BENCHMARK!!!";
        }
        bw.append(logLine);
        bw.newLine();
        bw.close();
    }

    public static void setTenancy(TestUserMode userMode) {

        if (userMode == TestUserMode.TENANT_ADMIN) {
            tenant = userMode.name();
        } else {
            tenant = SUPER_TENANT;
        }
    }

    public static void validateBenchmarkResults(String testName, String testType, LocalTime startTime, String scenario, String provider)
            throws InterruptedException, IOException, ParseException, APIManagerIntegrationTestException {

        int benchmark = getBenchmark(testType, scenario);
        int actualCount = extractCountsFromLog(testName, testType, startTime, provider);
        writeResultsToFile(testType, testName, actualCount, benchmark, provider);
        validateBenchmark(benchmark, actualCount, testType);
    }

}