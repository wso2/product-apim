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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class J2TemplateOverlayTest {

    private static final String REGISTRY_CACHE_OVERLAY =
            "<wso2registry>\n    <enableCache>false</enableCache>\n</wso2registry>\n";

    private static final String TEMPLATE = String.join("\n",
            "<wso2registry>",
            "    <enableCache>true</enableCache>",
            "    {% if database.config is defined%}",
            "    <dbConfig name=\"{{database.config}}\"/>",
            "    {% endif %}",
            "    <handler class=\"{{h.class}}\" {% if h.method is defined %} method=\"{{h.method}}\" {% endif %}>",
            "        <{{prop.tag}} key=\"{{prop.key}}\">{{prop.value}}</{{prop.tag}}>",
            "    </handler>",
            "    <remoteInstance>",
            "        <enableCache>{{governance_data.enable_cache}}</enableCache>",
            "    </remoteInstance>",
            "    <!--remoteInstance>",
            "        <enableCache>true</enableCache>",
            "    </remoteInstance-->",
            "</wso2registry>",
            "");

    @Test
    public void replacesOnlyTheAddressedLeafAndKeepsEveryOtherByte() {
        String merged = J2TemplateOverlay.merge(TEMPLATE, REGISTRY_CACHE_OVERLAY);

        Assert.assertEquals(merged, TEMPLATE.replaceFirst("<enableCache>true</enableCache>",
                "<enableCache>false</enableCache>"));
        Assert.assertTrue(merged.contains("<enableCache>{{governance_data.enable_cache}}</enableCache>"),
                "the nested remoteInstance value is a different path and must be untouched");
        Assert.assertTrue(merged.contains("<!--remoteInstance>\n        <enableCache>true</enableCache>"),
                "the commented-out sample must be untouched");
    }

    @Test
    public void appliesEveryLeafOfAMultiValueOverlay() {
        String template = "<root>\n  <a>1</a>\n  <b>\n    <c>2</c>\n  </b>\n</root>\n";
        String merged = J2TemplateOverlay.merge(template, "<root><a>10</a><b><c>20</c></b></root>");

        Assert.assertEquals(merged, "<root>\n  <a>10</a>\n  <b>\n    <c>20</c>\n  </b>\n</root>\n");
    }

    @Test
    public void keepsWhitespaceAroundTheValueAndEscapesTheNewValue() {
        String template = "<root>\n  <a>\n    old\n  </a>\n</root>";
        String merged = J2TemplateOverlay.merge(template, "<root><a>x &amp; y</a></root>");

        Assert.assertEquals(merged, "<root>\n  <a>\n    x &amp; y\n  </a>\n</root>");
    }

    @Test
    public void rejectsAPathMissingFromTheTemplate() {
        IllegalArgumentException e = Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(TEMPLATE, "<wso2registry><readOnly>true</readOnly></wso2registry>"));
        Assert.assertTrue(e.getMessage().contains("/wso2registry/readOnly"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("matched 0"), e.getMessage());
    }

    @Test
    public void rejectsAPathMatchedInBothBranchesOfAConditional() {
        String template = "<r>\n{% if x %}\n<a>1</a>\n{% else %}\n<a>2</a>\n{% endif %}\n</r>";
        IllegalArgumentException e = Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(template, "<r><a>3</a></r>"));
        Assert.assertTrue(e.getMessage().contains("matched 2"), e.getMessage());
    }

    @Test
    public void rejectsAValueRenderedFromDeploymentToml() {
        IllegalArgumentException e = Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(TEMPLATE,
                        "<wso2registry><remoteInstance><enableCache>false</enableCache></remoteInstance>"
                                + "</wso2registry>"));
        Assert.assertTrue(e.getMessage().contains("deployment.toml"), e.getMessage());
    }

    @Test
    public void rejectsAnElementThatIsNotAPlainTextLeaf() {
        Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge("<r><a><b>1</b></a></r>", "<r><a>2</a></r>"));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge("<r><a>1<!-- note --></a></r>", "<r><a>2</a></r>"));
    }

    @Test
    public void rejectsMalformedOrEmptyOverlays() {
        Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(TEMPLATE, "<wso2registry><enableCache>false</wso2registry>"));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(TEMPLATE, "<wso2registry><enableCache/></wso2registry>"));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> J2TemplateOverlay.merge(TEMPLATE,
                        "<wso2registry><enableCache>false</enableCache><enableCache>true</enableCache>"
                                + "</wso2registry>"));
    }

    @Test
    public void masksJinjaWithoutMovingAnyCharacter() {
        String masked = J2TemplateOverlay.mask(TEMPLATE);

        Assert.assertEquals(masked.length(), TEMPLATE.length());
        for (int i = 0; i < TEMPLATE.length(); i++) {
            if (TEMPLATE.charAt(i) == '\n') {
                Assert.assertEquals(masked.charAt(i), '\n', "newline moved at offset " + i);
            }
        }
        Assert.assertFalse(masked.contains("{%") || masked.contains("{{"));
    }

    @DataProvider
    public Object[][] productRegistryTemplates() {
        String templates = "modules/distribution/product/src/main/resources/conf/templates/repository/conf/"
                + "registry.xml.j2";
        return new Object[][] {
                {"all-in-one-apim/" + templates},
                {"api-control-plane/" + templates},
                {"gateway/" + templates},
                {"traffic-manager/" + templates},
        };
    }

    /** The shipped templates: only the top-level switch flips; commented samples and mount flags are untouched. */
    @Test(dataProvider = "productRegistryTemplates")
    public void disablesOnlyTheTopLevelRegistryCacheInTheProductTemplate(String relativePath) throws Exception {
        Path template = productRepositoryRoot().resolve(relativePath);
        String original = Files.readString(template);

        String merged = J2TemplateOverlay.merge(original, REGISTRY_CACHE_OVERLAY);

        String[] before = original.split("\n", -1);
        String[] after = merged.split("\n", -1);
        Assert.assertEquals(after.length, before.length);
        int changed = 0;
        for (int i = 0; i < before.length; i++) {
            if (!before[i].equals(after[i])) {
                changed++;
                Assert.assertEquals(before[i].trim(), "<enableCache>true</enableCache>", "line " + (i + 1));
                Assert.assertEquals(after[i], before[i].replace("true", "false"), "line " + (i + 1));
                Assert.assertTrue(i < 40, "the top-level switch is near the top; changed line " + (i + 1));
            }
        }
        Assert.assertEquals(changed, 1, "exactly one line must change in " + relativePath);
    }

    private static Path productRepositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("all-in-one-apim"))
                    && Files.isDirectory(current.resolve("traffic-manager"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate the product-apim repository root from "
                + System.getProperty("user.dir"));
    }
}
