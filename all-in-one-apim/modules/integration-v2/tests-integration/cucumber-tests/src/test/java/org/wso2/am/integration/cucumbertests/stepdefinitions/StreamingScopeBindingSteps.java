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
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Steps for the scope role bindings ({@code x-scopes-bindings}) of streaming (AsyncAPI) APIs: reading and replacing
 * an API's AsyncAPI definition, building and importing an API project archive, editing an exported archive, and
 * reading a scope as registered in the resident key manager.
 */
public class StreamingScopeBindingSteps {

    private static final String X_SCOPES_BINDINGS = "x-scopes-bindings";

    /**
     * Zips a classpath directory into an API project archive whose root folder has the directory's name. Every
     * file is read as UTF-8 text with its {@code {{...}}} placeholders resolved, so the project can carry
     * scenario-unique API, context and scope names. Stores the archive path under {@code archiveKey}.
     */
    @When("I build an API project archive from directory {string} as {string}")
    public void iBuildApiProjectArchive(String directory, String archiveKey) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource(directory);
        if (url == null) {
            throw new FileNotFoundException("Project directory not found on classpath: " + directory);
        }
        Path root = Paths.get(url.toURI());
        File archive = File.createTempFile("api-project", ".zip");
        archive.deleteOnExit();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
        Assert.assertFalse(files.isEmpty(), "Project directory " + directory + " has no files");
        String rootName = root.getFileName().toString();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archive))) {
            for (Path file : files) {
                String entryName = rootName + "/" + root.relativize(file).toString().replace(File.separatorChar, '/');
                zos.putNextEntry(new ZipEntry(entryName));
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                zos.write(Utils.resolveContextPlaceholders(text).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        TestContext.set(Utils.normalizeContextKey(archiveKey), archive.getAbsolutePath());
    }

    /**
     * Imports an API project archive ({@code POST /apis/import?overwrite=}) as the acting actor and publishes the
     * response. The import response carries no id, so the feature resolves the API afterwards with
     * {@code I find the Publisher API named ...}, which also registers it for teardown.
     */
    @When("I import the API project archive {string} with overwrite {string}")
    public void iImportApiProjectArchive(String archiveKey, String overwrite) throws IOException {
        File archive = new File(TestContext.resolve(archiveKey).toString());
        Map<String, File> files = new HashMap<>();
        files.put("file", archive);
        Requests.postMultipart(Utils.getApiArchiveImportURL(Utils.getBaseUrl(), Boolean.parseBoolean(overwrite)),
                Identity.publisherHeaders(), files, null);
    }

    /** Retrieves an API's stored AsyncAPI definition ({@code GET /apis/{id}/asyncapi}) and publishes it. */
    @When("I retrieve the AsyncAPI definition of API {string}")
    public void iRetrieveAsyncApiDefinition(String apiIdKey) throws IOException {
        Requests.get(Utils.getAsyncApiDefinitionURL(Utils.getBaseUrl(), TestContext.resolve(apiIdKey).toString()),
                Identity.publisherHeaders());
    }

    /**
     * Replaces an API's AsyncAPI definition ({@code PUT /apis/{id}/asyncapi}, multipart {@code file}) with a classpath
     * definition whose {@code {{...}}} placeholders are resolved. Non-asserting — the feature asserts the status.
     */
    @When("I update the AsyncAPI definition of API {string} from file {string}")
    public void iUpdateAsyncApiDefinition(String apiIdKey, String filepath) throws IOException {
        File definition = File.createTempFile("asyncapi", ".yaml");
        definition.deleteOnExit();
        Files.write(definition.toPath(), Utils.resolveContextPlaceholders(Utils.readClasspathResource(filepath))
                .getBytes(StandardCharsets.UTF_8));
        Map<String, File> files = new HashMap<>();
        files.put("file", definition);
        Requests.putMultipart(Utils.getAsyncApiDefinitionURL(Utils.getBaseUrl(),
                TestContext.resolve(apiIdKey).toString()), Identity.publisherHeaders(), files, new HashMap<>());
    }

    /**
     * Asserts the AsyncAPI definition in the response binds {@code scope} to exactly {@code role} in the oauth2
     * implicit flow's {@code x-scopes-bindings}. Both arguments resolve {@code {{...}}}.
     */
    @Then("The AsyncAPI definition in the response should bind scope {string} to role {string}")
    public void theDefinitionShouldBindScope(String scope, String role) {
        String scopeName = Utils.resolveContextPlaceholders(scope);
        JSONObject bindings = scopeBindings();
        Assert.assertNotNull(bindings, "The AsyncAPI definition has no " + X_SCOPES_BINDINGS + "; expected "
                + scopeName + " -> " + role + ". Definition: " + responseDefinition());
        Assert.assertTrue(bindings.has(scopeName), "No " + X_SCOPES_BINDINGS + " entry for scope " + scopeName
                + "; expected role " + role + ". Bindings: " + bindings);
        Assert.assertEquals(String.valueOf(bindings.get(scopeName)), Utils.resolveContextPlaceholders(role),
                "Wrong role bound to scope " + scopeName + ". Bindings: " + bindings);
    }

    /**
     * Asserts the AsyncAPI definition in the response has NO {@code x-scopes-bindings} entry for {@code scope}: a
     * scope without a role is left out (not written as {@code ""}), so an exported project can take its role from
     * {@code api.yaml} on re-import.
     */
    @Then("The AsyncAPI definition in the response should have no binding for scope {string}")
    public void theDefinitionShouldNotBindScope(String scope) {
        String scopeName = Utils.resolveContextPlaceholders(scope);
        JSONObject bindings = scopeBindings();
        Assert.assertTrue(bindings == null || !bindings.has(scopeName), "Expected no " + X_SCOPES_BINDINGS
                + " entry for scope " + scopeName + ", but found: " + bindings);
    }

    /**
     * Rewrites every entry of an archive whose name contains {@code entryPart}, replacing each match of the Java
     * regex {@code pattern} with {@code replacement} (both resolve {@code {{...}}}). Fails if nothing matched, so a
     * changed export format cannot turn the edit into a silent no-op. The archive is rewritten in place.
     */
    @When("I edit the entry {string} of archive {string} replacing pattern {string} with {string}")
    public void iEditArchiveEntry(String entryPart, String archiveKey, String pattern, String replacement)
            throws IOException {
        File archive = new File(TestContext.resolve(archiveKey).toString());
        Pattern regex = Pattern.compile(Utils.resolveContextPlaceholders(pattern));
        String resolvedReplacement = Matcher.quoteReplacement(Utils.resolveContextPlaceholders(replacement));
        File edited = File.createTempFile("api-project-edited", ".zip");
        edited.deleteOnExit();
        int replaced = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(edited))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] data = zis.readAllBytes();
                if (!entry.isDirectory() && entry.getName().contains(entryPart)) {
                    Matcher matcher = regex.matcher(new String(data, StandardCharsets.UTF_8));
                    StringBuilder out = new StringBuilder();
                    while (matcher.find()) {
                        matcher.appendReplacement(out, resolvedReplacement);
                        replaced++;
                    }
                    matcher.appendTail(out);
                    data = out.toString().getBytes(StandardCharsets.UTF_8);
                }
                zos.putNextEntry(new ZipEntry(entry.getName()));
                zos.write(data);
                zos.closeEntry();
            }
        }
        Assert.assertTrue(replaced > 0, "Pattern '" + pattern + "' matched nothing in entries containing '"
                + entryPart + "' of " + archive);
        TestContext.set(Utils.normalizeContextKey(archiveKey), edited.getAbsolutePath());
    }

    /** Asserts no entry of an archive whose name contains {@code entryPart} contains {@code text}. */
    @Then("The entry {string} of archive {string} should not contain {string}")
    public void theArchiveEntryShouldNotContain(String entryPart, String archiveKey, String text) throws IOException {
        File archive = new File(TestContext.resolve(archiveKey).toString());
        String expectedAbsent = Utils.resolveContextPlaceholders(text);
        boolean found = false;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().contains(entryPart)) {
                    continue;
                }
                found = true;
                String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                Assert.assertFalse(content.contains(expectedAbsent), "Entry " + entry.getName() + " of " + archive
                        + " contains '" + expectedAbsent + "':\n" + content);
            }
        }
        Assert.assertTrue(found, "No entry containing '" + entryPart + "' in " + archive);
    }

    /**
     * Retrieves a scope as registered in the resident key manager
     * ({@code GET /api/identity/oauth2/v1.0/scopes/name/{name}}, tenant-qualified for a tenant actor) with the acting
     * actor's basic credentials, and publishes the response. Its {@code bindings} are the roles the token endpoint
     * enforces for the scope. The acting actor must be a tenant admin.
     */
    @When("I retrieve the key manager scope {string}")
    public void iRetrieveKeyManagerScope(String scope) throws IOException {
        Requests.get(Utils.getKeyManagerScopeURL(Utils.getBaseUrl(), Identity.actingTenantDomain(),
                Utils.resolveContextPlaceholders(scope)), Identity.actingBasicAuthHeaders());
    }

    /** The oauth2 implicit flow's {@code x-scopes-bindings} of the definition in the response, or null if absent. */
    private static JSONObject scopeBindings() {
        JSONObject definition = new JSONObject(responseDefinition());
        JSONObject implicit = definition;
        for (String key : new String[]{"components", "securitySchemes", "oauth2", "flows", "implicit"}) {
            implicit = implicit.optJSONObject(key);
            Assert.assertNotNull(implicit, "The AsyncAPI definition has no oauth2 implicit flow (missing '" + key
                    + "'): " + definition);
        }
        return implicit.optJSONObject(X_SCOPES_BINDINGS);
    }

    /** The body of a successful definition response, failing clearly on an error or empty response. */
    private static String responseDefinition() {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx AsyncAPI definition response, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        return response.getData();
    }
}
