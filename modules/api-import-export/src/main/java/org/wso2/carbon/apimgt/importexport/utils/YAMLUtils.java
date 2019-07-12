/*
 *
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * /
 */

package org.wso2.carbon.apimgt.importexport.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;

/**
 * Used to handle YAML related operations
 */
public class YAMLUtils {
    /**
     * Converts a YAML file into JSON
     *
     * @param yaml yaml representation
     * @return yaml file as a json
     * @throws IOException
     */
    public static String YamlToJson(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    /**
     * Converts JSON to YAML
     *
     * @param json json representation
     * @return json file as a yaml document
     * @throws IOException
     */
    public static String JsonToYaml(String json) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory().enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
        JsonNode jsonNodeTree = yamlReader.readTree(json);
        YAMLMapper yamlMapper = new YAMLMapper()
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS)
                .disable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
        return yamlMapper.writeValueAsString(jsonNodeTree);
    }
}

