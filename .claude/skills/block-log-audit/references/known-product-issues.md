# Known product issues (cite by ID; re-verify before citing)

Established in earlier block audits against `wso2am:4.7.0-SNAPSHOT` (carbon-apimgt 9.33.x). A newer product build may
have fixed any of them: before citing an ID, confirm the log line and, where it matters, the bytecode still match the
signature. If they no longer match, investigate as new and note that the catalogue entry may be stale.

| ID | Signature in the logs | Mechanism | Test-side stance |
|---|---|---|---|
| PI-1 | API create 201 then permanent 404; delete 500 NPE | Registry: deleting an owner's last API removes the provider folder from under a concurrent create (half-created API) | Not a test leak; report |
| PI-2 | FK violation / generic 500 on two first creates for one provider | Registry: two first creates collide creating the provider folder | Report |
| PI-3 | mTLS 401 for the whole window after a listener SSL reload | Synapse `reloadDynamicSSLConfig` removes `SSLProfiles` before the started-state check; an early reload disables later reloads | The suite's readiness gate restarts once when needed |
| PI-4 | `KeyManagerJMSMessageListener` RuntimeException (Auth0 401, IS7/WSO2-IS NPE `keyManagerServiceUrl is null`) | `onMessage` does not catch RuntimeExceptions; missing endpoints in a KM config | Supply complete KM endpoint configs in fixtures |
| PI-5 | 24× governance WARN after MCP server deletes; `GOV_ARTIFACT` rows left | MCP delete clears governance data with type `"MCP_SERVER"`, which `ArtifactType.fromString` maps to null | Report |
| PI-6 | `Axis2Sender` double-response WARN after schema-validation threat fault | `SchemaValidator.handleResponse` returns true after the fault sequence already responded | Report |
| PI-7 | `ConcurrentModificationException` in `AMQSession$2.run` | andes-client iterates a synchronized map without its lock | Report |
| PI-8 | "Error while Store the Revision Artifact"; partial revision rows | API export uses a fixed per-API temp directory; concurrent exports (governance + revision) collide | Report |
| PI-9 | Gateway deployments blocked for minutes; "No X509TrustManager" | Endpoint-certificate fetch retry loop after certificate events | Order certificate scenarios so later deploys don't wait on them |
| PI-10 | Deprecated-API subscribe returns 500 | Should be 404 | Pinned in `devportal/subscribe.feature` |
| PI-11 | Deactivated tenant still authenticates intermittently | `JDBCTenantManager.deactivateTenant/activateTenant` clear the tenant cache before `UM_ACTIVE` commits; a concurrent read re-caches the old row | Poll the effective state |
| PI-12 | Change-owner 500 "User ... doesn't exist in user store" | `APIConsumerImpl.updateApplicationOwner` requires the target to be a DevPortal subscriber already | Make the target a subscriber first (any DevPortal call with the subscribe scope) |
| PI-13 | `NotificationEventSenderService`/`getEventSender()` null NPE on OAuth app removal | `APIMOAuthApplicationMgtListener` has no null guard; the sender exists only when the default `token_revocation` event listener is configured | Not a product bug in a correct config: restate the default listener in overlays (see defect C4) |
| PI-14 | Key-mapping delete returns 500 for an already-removed mapping | 500 instead of 404 | Avoid the re-delete (defect D2) |
