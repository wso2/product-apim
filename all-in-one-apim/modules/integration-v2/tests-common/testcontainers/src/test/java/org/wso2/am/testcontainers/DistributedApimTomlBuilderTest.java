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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DistributedApimTomlBuilderTest {

    @Test
    public void shouldApplyRuntimeValuesAfterAllOverlays() throws Exception {
        String result = DistributedApimTomlBuilder.build(
                "[server]\nhostname=\"localhost\"\n[database.shared_db]\nurl=\"h2\"\n",
                "[server]\nhostname=\"apim-cp\"\n[database.shared_db]\ntype=\"mysql\"\n",
                "[server]\nhostname=\"overlay-host\"\n",
                Map.of("server.hostname", "apim-cp-final",
                        "database.shared_db.url", "jdbc:mysql://mysql:3306/WSO2AM_SHARED_DB?useSSL=false"));

        Assert.assertTrue(result.contains("apim-cp-final"));
        Assert.assertTrue(result.contains("jdbc:mysql://mysql:3306/WSO2AM_SHARED_DB?useSSL=false"));
        Assert.assertFalse(result.contains("overlay-host"));
    }

    @Test
    public void shouldPreserveEncryptionAsAnExplicitTable() throws Exception {
        String result = DistributedApimTomlBuilder.build(
                "[server]\nhostname=\"localhost\"\n",
                "[server]\nhostname=\"apim-cp\"\n",
                "[encryption]\nkey=\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\"\n",
                Map.of());

        Assert.assertTrue(result.contains("[encryption]"));
        Assert.assertTrue(result.contains("key = '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'")
                || result.contains("key = \"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\""));
        Assert.assertFalse(result.contains("encryption.key ="));
    }

    @Test
    public void shouldRejectMissingArrayParentInRuntimePath() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> DistributedApimTomlBuilder.build(
                        "[server]\nhostname=\"localhost\"\n",
                        "[server]\n\n",
                        null,
                        Map.of("server.missing.0.value", "invalid")));
    }

    @Test
    public void shouldCreateMissingObjectParentForNonIndexPath() throws Exception {
        String result = DistributedApimTomlBuilder.build(
                "[server]\nhostname=\"localhost\"\n",
                "[server]\n\n",
                null,
                Map.of("server.missing.value", "valid"));

        JsonNode value = new TomlMapper().readTree(result).at("/server/missing/value");
        Assert.assertEquals(value.asText(), "valid");
    }

    @Test
    public void shouldRejectAmbiguousDistributedOverlayParameters() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> DistributedApimTomlBuilder.resolveExtraOverlay(
                        Map.of("tomlExtraOverlayPath", "legacy.toml"),
                        DistributedApimTomlBuilder.Component.CP));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> DistributedApimTomlBuilder.resolveServerFiles(
                        Map.of("serverFilesToCopy", "a::b"),
                        DistributedApimTomlBuilder.Component.GATEWAY));
    }

    @Test
    public void shouldResolveOnlyFilesQualifiedForTheSelectedComponent() throws Exception {
        Path source = Files.createTempFile("distributed-apim", ".xml");
        Files.writeString(source, "fixture");
        String value = source + "::repository/conf/example.xml";

        List<DistributedApimTomlBuilder.ServerFile> files =
                DistributedApimTomlBuilder.resolveServerFiles(
                        Map.of("serverFilesToCopy.cp", value),
                        DistributedApimTomlBuilder.Component.CP);

        Assert.assertEquals(files.size(), 1);
        Assert.assertEquals(files.get(0).serverRelativePath(), "repository/conf/example.xml");
        Assert.assertTrue(DistributedApimTomlBuilder.resolveServerFiles(
                Map.of("serverFilesToCopy.cp", value),
                DistributedApimTomlBuilder.Component.TM).isEmpty());
    }

    @Test
    public void distributedBaseOverlaysMustUseNetworkAliases() throws Exception {
        for (String resource : new String[]{"cp-base-overlay.toml", "tm-base-overlay.toml",
                "gateway-base-overlay.toml"}) {
            String content = new String(getClass().getClassLoader()
                    .getResourceAsStream("distributed-apim/" + resource).readAllBytes());
            Assert.assertFalse(content.contains("https://localhost:9443/services/"),
                    resource + " uses localhost for an internal APIM service endpoint");
            Assert.assertFalse(content.contains("jdbc:mysql://"),
                    resource + " must not hardcode runtime database URLs");
        }

        String cpOverlay = new String(getClass().getClassLoader().getResourceAsStream(
                "distributed-apim/cp-base-overlay.toml").readAllBytes());
        String generated = DistributedApimTomlBuilder.build(
                "[database.apim_db]\nurl=\"h2\"\n",
                cpOverlay, null, Map.of(
                        "database.apim_db.url", "jdbc:mysql://mysql:3306/WSO2AM_DB?autoReconnect=true"
                                + "&amp;allowPublicKeyRetrieval=true&amp;useSSL=false",
                        "database.shared_db.url", "jdbc:mysql://mysql:3306/WSO2AM_SHARED_DB?autoReconnect=true"
                                + "&amp;allowPublicKeyRetrieval=true&amp;useSSL=false"));
        JsonNode urlNode = new TomlMapper().readTree(generated).at("/database/apim_db/url");
        Assert.assertEquals(urlNode.asText(),
                "jdbc:mysql://mysql:3306/WSO2AM_DB?autoReconnect=true&amp;allowPublicKeyRetrieval=true"
                        + "&amp;useSSL=false",
                "Generated TOML must preserve the product-required XML-escaped JDBC URL");
    }
}
