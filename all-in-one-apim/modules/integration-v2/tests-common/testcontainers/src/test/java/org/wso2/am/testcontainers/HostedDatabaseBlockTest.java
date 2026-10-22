/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.testcontainers;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

public class HostedDatabaseBlockTest {

    @Test
    public void normalizesOracleRacProfileAliases() {
        Assert.assertEquals(HostedDatabaseBlock.normalizeProfile("oracle-rac"), "oracle-rac");
        Assert.assertEquals(HostedDatabaseBlock.normalizeProfile("oracle_rac"), "oracle-rac");
        Assert.assertEquals(HostedDatabaseBlock.normalizeProfile("oraclerac"), "oracle-rac");
    }

    @Test
    public void acceptsOracle23cProfile() {
        Assert.assertEquals(HostedDatabaseBlock.normalizeProfile("oracle_23c"), "oracle_23c");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsUnqualifiedOracleProfile() {
        HostedDatabaseBlock.normalizeProfile("oracle");
    }

    @Test
    public void splitsDb2SuffixTerminatorsWithoutSplittingTriggerBodies() throws Exception {
        List<String> statements = splitDb2("CREATE TABLE ITEMS (ID INTEGER)/\n"
                + "CREATE SEQUENCE ITEMS_SEQ START WITH 1\n/\n"
                + "CREATE TRIGGER ITEMS_TRIGGER NO CASCADE BEFORE INSERT ON ITEMS\n"
                + "REFERENCING NEW AS NEW FOR EACH ROW MODE DB2SQL\n"
                + "BEGIN ATOMIC\n SET NEW.ID = NEXTVAL FOR ITEMS_SEQ;\nEND/\n");

        Assert.assertEquals(statements.size(), 3);
        Assert.assertEquals(statements.get(0), "CREATE TABLE ITEMS (ID INTEGER)");
        Assert.assertEquals(statements.get(1), "CREATE SEQUENCE ITEMS_SEQ START WITH 1");
        Assert.assertTrue(statements.get(2).endsWith("SET NEW.ID = NEXTVAL FOR ITEMS_SEQ;\nEND"));
    }

    @Test
    public void ignoresDb2TerminatorsInsideCommentsAndQuotedValues() throws Exception {
        List<String> statements = splitDb2("/* product schema / */\n"
                + "CREATE TABLE ITEMS (VALUE VARCHAR(100)) / -- table /\n"
                + "-- A registry path must remain one literal, including its final slash. /\n"
                + "INSERT INTO ITEMS VALUES ('/apim/it''s-a-path/') /\n"
                + "INSERT INTO ITEMS VALUES ('\\') /\n"
                + "INSERT INTO ITEMS VALUES ('value;/') /\n");

        Assert.assertEquals(statements, List.of("CREATE TABLE ITEMS (VALUE VARCHAR(100))",
                "INSERT INTO ITEMS VALUES ('/apim/it''s-a-path/')", "INSERT INTO ITEMS VALUES ('\\')",
                "INSERT INTO ITEMS VALUES ('value;/')"));
    }

    @SuppressWarnings("unchecked")
    private static List<String> splitDb2(String script) throws Exception {
        Method method = HostedDatabaseBlock.class.getDeclaredMethod("splitDb2Statements", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(null, script);
    }
}
