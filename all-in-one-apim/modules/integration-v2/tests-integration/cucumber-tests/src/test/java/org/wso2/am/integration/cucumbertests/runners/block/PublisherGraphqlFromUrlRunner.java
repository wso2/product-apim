/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *  Licensed under the Apache License, Version 2.0 (the "License"); ...
 */
package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/** Runner for GraphQL API creation from a URL (introspection + SDL fetch) — ports GraphqlTestCase. */
@CucumberOptions(
        features = {"src/test/resources/features/publisher/graphql_design_from_url.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/publisher-graphql-from-url.html"}
)
public class PublisherGraphqlFromUrlRunner extends BaseBlockRunner {
}
