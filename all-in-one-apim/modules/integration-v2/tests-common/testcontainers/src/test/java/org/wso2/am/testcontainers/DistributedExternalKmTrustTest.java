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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

/** Verifies staged files and effective URLs without starting Docker containers or depending on product ZIPs. */
public class DistributedExternalKmTrustTest {

    @DataProvider
    public Object[][] modes() {
        return new Object[][]{{false, false}, {true, false}, {true, true}};
    }

    @Test(dataProvider = "modes")
    public void shouldStageTrustAndUseTheCertificateCoveredCpAuthority(boolean externalTrust,
                                                                      boolean notifications) throws Exception {
        Path directory = Files.createTempDirectory("distributed-km-trust-");
        String previousTruststore = System.getProperty("apim.km.truststore.path");
        try (Network network = Network.newNetwork()) {
            Path[] defaults = new Path[3];
            String[] names = {"cp", "tm", "gateway"};
            for (int i = 0; i < names.length; i++) {
                Path component = Files.createDirectories(directory.resolve(names[i]));
                defaults[i] = component.resolve("deployment.toml");
                Files.writeString(defaults[i], "[server]\nhostname=\"localhost\"\n"
                        + "[[apim.gateway.environment]]\nname=\"Default\"\n");
                Path template = component.resolve("templates/repository/conf/registry.xml.j2");
                Files.createDirectories(template.getParent());
                Files.writeString(template, "<wso2registry><enableCache>true</enableCache></wso2registry>");
            }
            Path schema = Files.writeString(directory.resolve("schema.sql"), "-- fixture");
            byte[] truststore = "augmented-truststore-fixture".getBytes(StandardCharsets.UTF_8);
            System.setProperty("apim.km.truststore.path",
                    Files.write(directory.resolve("client-truststore.jks"), truststore).toString());

            Constructor<DistributedDynamicApimContainer> constructor = DistributedDynamicApimContainer.class
                    .getDeclaredConstructor(String.class, Path.class, Path.class, Path.class, Path.class,
                            Path.class, Network.class, boolean.class);
            constructor.setAccessible(true);
            DistributedDynamicApimContainer runtime = constructor.newInstance("km-trust", defaults[0], defaults[1],
                    defaults[2], schema, schema, network, false);
            runtime.withTomlExtraOverlay(DistributedApimTomlBuilder.Component.CP,
                    "[apim.throttling]\nenable_policy_deployment=true\n");
            runtime.withTomlExtraOverlay(DistributedApimTomlBuilder.Component.TM,
                    "[apim.throttling]\nenable_policy_deployment=false\n");
            if (externalTrust) {
                runtime.withExternalKmTrust();
            }
            if (notifications) {
                runtime.withExternalIsNotificationAlias();
            }
            Method create = DistributedDynamicApimContainer.class.getDeclaredMethod("createComponents");
            create.setAccessible(true);
            create.invoke(runtime);

            String expectedServices = "https://" + (notifications ? "wso2am" : "apim-cp") + ":9443/services/";
            for (String name : names) {
                Field field = DistributedDynamicApimContainer.class.getDeclaredField(name);
                field.setAccessible(true);
                GenericContainer<?> container = (GenericContainer<?>) field.get(runtime);
                Map<String, byte[]> files = stagedFiles(container);
                String truststorePath = "/opt/wso2/repository/resources/security/client-truststore.jks";
                if (externalTrust) {
                    Assert.assertEquals(files.get(truststorePath), truststore,
                            name + " must receive the augmented truststore before startup");
                } else {
                    Assert.assertFalse(files.containsKey(truststorePath),
                            name + " must retain its image truststore in ordinary blocks");
                }
                JsonNode toml = new TomlMapper().readTree(new String(
                        files.get("/opt/wso2/repository/conf/deployment.toml"), StandardCharsets.UTF_8));
                Assert.assertEquals(toml.at("/apim/event_hub/service_url").asText(), expectedServices, name);
                if (name.equals("cp")) {
                    Assert.assertFalse(toml.at("/apim/throttling/enable_policy_deployment").asBoolean());
                    Assert.assertEquals(toml.at("/apim/key_manager/service_url").asText(), expectedServices);
                    Assert.assertEquals(container.getNetworkAliases().contains("wso2am"), notifications);
                }
                if (name.equals("tm")) {
                    Assert.assertTrue(toml.at("/apim/throttling/enable_policy_deployment").asBoolean());
                }
                Assert.assertEquals(toml.at("/server/hostname").asText(), "apim-" +
                        (name.equals("gateway") ? "gw" : name));
            }
        } finally {
            if (previousTruststore == null) {
                System.clearProperty("apim.km.truststore.path");
            } else {
                System.setProperty("apim.km.truststore.path", previousTruststore);
            }
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static Map<String, byte[]> stagedFiles(GenericContainer<?> container) throws Exception {
        Method method = GenericContainer.class.getDeclaredMethod("getCopyToTransferableContainerPathMap");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Transferable, String> staged = (Map<Transferable, String>) method.invoke(container);
        Map<String, byte[]> files = new java.util.HashMap<>();
        staged.forEach((content, path) -> files.put(path, content.getBytes()));
        return files;
    }
}
