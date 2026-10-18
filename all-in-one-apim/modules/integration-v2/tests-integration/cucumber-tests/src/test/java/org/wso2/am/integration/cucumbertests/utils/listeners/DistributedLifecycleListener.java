/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.testng.ITestContext;
import org.testcontainers.containers.Network;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.RegistryAdminService;
import org.wso2.am.integration.cucumbertests.utils.RegistryBootstrapDiagnostics;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.ThrottleDataReadiness;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DistributedApimTomlBuilder;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;
import org.wso2.am.testcontainers.DynamicISContainer;
import org.wso2.carbon.automation.engine.context.beans.User;

import java.io.IOException;
import java.util.Map;

/** Block lifecycle variant that swaps the all-in-one APIM runtime for the distributed composite. */
public class DistributedLifecycleListener extends BlockLifecycleListener {

    private static final String EXTERNAL_IS_HEAP_OPTS = "-Xms750m -Xmx1g";

    @Override
    protected DynamicISContainer createExternalIdentityServer(String label, String extraOverlayContent) {
        return new DynamicISContainer(label, extraOverlayContent)
                .withEnv("JVM_MEM_OPTS", EXTERNAL_IS_HEAP_OPTS);
    }

    @Override
    protected ApimRuntime createApimContainer(String label, ITestContext context, Network blockNetwork)
            throws IOException {
        if (param(context, PARAM_TOML_OVERLAY) != null && !param(context, PARAM_TOML_OVERLAY).isBlank()) {
            throw new IllegalArgumentException("Distributed APIM does not accept full-file tomlOverlayPath; use "
                    + "tomlExtraOverlayPath.cp, .tm, or .gateway");
        }
        // Every distributed Gateway starts both broker consumers, independently of the block's readiness flag.
        // Establish the shared collection while CP is its only writer and TM's path cache is still empty.
        DistributedDynamicApimContainer runtime = new DistributedDynamicApimContainer(label, blockNetwork);
        return runtime.withControlPlaneInitializer(baseUrl -> {
            TenantUserProvisioner.addSuperTenant();
            User actor = Identity.resolveActor("admin");
            RegistryAdminService.awaitAvailable(baseUrl, actor, Constants.SERVER_STARTUP_WAIT_TIME);
            RegistryAdminService.ensureThrottleDataHierarchy(baseUrl, actor,
                    Constants.SERVER_STARTUP_WAIT_TIME,
                    RegistryBootstrapDiagnostics.forRuntime(runtime, label, "carbon.super"));
        });
    }

    /** The supertenant broker hierarchy already exists; retain tenant setup without restarting components. */
    @Override
    protected void bootstrapThrottleDataInfrastructure(ApimRuntime runtime, String label, boolean tenantUsersEnabled)
            throws Exception {
        if (tenantUsersEnabled) {
            RegistryAdminService.ensureThrottleDataHierarchy(runtime.getServletHttpsUrl(),
                    Identity.resolveActor("admin@tenant1.com"),
                    RegistryBootstrapDiagnostics.forRuntime(runtime, label, "tenant1.com"));
        }
        ThrottleDataReadiness.Result readiness = ThrottleDataReadiness.await(runtime, 180);
        if (readiness == null || !readiness.ready()) {
            throw new IllegalStateException("Distributed throttle-data infrastructure for block '" + label
                    + "' did not become ready after initialization: "
                    + (readiness == null ? "no readiness sample" : readiness.diagnostics()));
        }
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
                if (Boolean.parseBoolean(param(context, PARAM_EMAIL_USER_MODE))) {
                    container.withTomlExtraOverlay(component, emailUserOverlayContent());
                }
                String overlay = DistributedApimTomlBuilder.resolveExtraOverlay(parameters, component);
                if (overlay != null) {
                    container.withTomlExtraOverlay(component, overlay);
                }
                for (DistributedApimTomlBuilder.ServerFile file
                        : DistributedApimTomlBuilder.resolveServerFiles(parameters, component)) {
                    container.withComponentServerFile(component, file);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to resolve distributed "
                        + component.parameterName() + " configuration", e);
            }
        }
    }
}
