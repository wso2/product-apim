/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.testng.ITestContext;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DistributedApimTomlBuilder;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.Map;

/** Block lifecycle variant that swaps the all-in-one APIM runtime for the distributed composite. */
public class DistributedLifecycleListener extends BlockLifecycleListener {

    @Override
    protected ApimRuntime createApimContainer(String label, ITestContext context, Network blockNetwork)
            throws IOException {
        if (param(context, PARAM_TOML_OVERLAY) != null && !param(context, PARAM_TOML_OVERLAY).isBlank()) {
            throw new IllegalArgumentException("Distributed APIM does not accept full-file tomlOverlayPath; use "
                    + "tomlExtraOverlayPath.cp, .tm, or .gateway");
        }
        return new DistributedDynamicApimContainer(label, blockNetwork);
    }

    @Override
    protected boolean awaitApimReady(ApimRuntime runtime) {
        // CP-only distributions do not expose the all-in-one gateway health API. The management login page
        // proves that CP's HTTPS listener and web application layer are serving; TM/Gateway are independently
        // gated by their Carbon startup wait strategies in the composite.
        return org.wso2.am.integration.cucumbertests.utils.ServerReadiness.awaitHttpEndpoint(
                runtime.getServletHttpsUrl() + "carbon/admin/login.jsp");
    }

    @Override
    protected void configureServerFiles(ITestContext context, ApimRuntime runtime) {
        DistributedDynamicApimContainer container = (DistributedDynamicApimContainer) runtime;
        Map<String, String> parameters = context.getCurrentXmlTest().getLocalParameters();
        for (DistributedApimTomlBuilder.Component component : DistributedApimTomlBuilder.Component.values()) {
            try {
                String overlay = DistributedApimTomlBuilder.resolveExtraOverlay(parameters, component);
                if (overlay != null) {
                    container.withTomlExtraOverlay(component, overlay);
                }
                for (DistributedApimTomlBuilder.ServerFile file
                        : DistributedApimTomlBuilder.resolveServerFiles(parameters, component)) {
                    container.withComponentServerFile(component, file.source().toString(), file.serverRelativePath());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to resolve distributed "
                        + component.parameterName() + " configuration", e);
            }
        }
    }
}
