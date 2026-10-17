# Benign and expected messages

Messages established as harmless in earlier block audits of this suite (APIM 4.7.0-SNAPSHOT, carbon-apimgt 9.33.x).
Each entry states **why** it is harmless and **when it stops being harmless**. Before citing an entry, check that the
log line matches its signature and that its condition holds in this run; otherwise investigate it as new.

## Startup noise (list once per report)

During server startup, before the block's first scenario:
- `Digester` could not set `maxDays`; `DefaultSchemaGenerator` "We don't support method overloading. Ignoring ..."
  (`addRemoteServerConfig`, `resetRemoteServerConfig`, …).
- Missing identity auth templates / default tenant error page; blank `AuthenticationService` notice; missing
  `OrganizationCreationHandler` properties; Nashorn unavailable; adaptive authentication disabled.
- `Waiting for required OSGi services`.
- `AccessConfiguration` / `MiscellaneousUtil` unable to load `access-log.properties` (the server falls back).
- `APIMgtGoogleAnalyticsTrackingHandler Unable to load Google Analytics configuration using key: ga-config-key`
  (optional GA config absent).
- `TenantServiceCreator Error while copying API manager specific synapse sequences ...` (copy race at tenant load).
*Stops being benign*: the same message after startup in the middle of a scenario, or with a stack trace that ends in a
test-visible failure.

## Server side of asserted negatives

Validation failures, malformed context (`APIUtil The API context is malformed`), already-exists, invalid or unknown
ids, scope/subscription/credential denials (`OAuthOpaqueAuthenticatorImpl You cannot access API as scope validation
failed`, `APIAuthenticationHandler ... The access token does not allow you to access the requested resource`,
`MutualSSLAuthenticator Mutual SSL authentication failure`), GraphQL depth/complexity rejections, schema validation,
deny-policy input errors, `Unknown tier` when the tier is the scenario's subject, permission refusals
(`You don't have permission to access the application with Id ...`).
*Benign only if*: a step deliberately provokes it **and** the scenario asserts the outcome **and** the logged reason is
the reason the scenario claims (otherwise it is an A finding). Name the asserting step in the G row.

## Post-delete and propagation races (harmless by construction)

- `InMemoryAPIDeployer ... Storage returned null` / `Error deploying <id> in Gateway`: a deploy event processed after
  the scenario's cleanup already undeployed/deleted the API (5 s `event_waiting_time` per event, serial).
  *Stops being benign*: the API is still expected to be live (the scenario invokes it afterwards), or the id belongs to
  an API that should be deployed at that moment — then it is A3/E1.
- Governance/indexer "artifact not found" after deletes; `ComplianceEvaluationScheduler Error processing evaluation
  request for artifact <id>` for an already-deleted artifact; `ApiMgtDAO Unable to find the API with UUID` for a
  just-deleted API.
- `ApiMgtDAO Unable to find provider` on the first import for a new provider.
- `RPCMessageReceiver Resource does not exist at path /_system/governance/event...` when the scenario probes absence.
*Stops being benign*: the id is not one the scenario (or its cleanup) just deleted.

## Concurrency signals that are not errors

- `SimpleHTTPClient publisher-lock: <METHOD> <path> waited <n>ms for an overlapping Publisher request on the same
  server` (INFO): parallel classes on one server serialise Publisher writes by design. *Stops being benign*: a wait
  near the lock's deadline, or a request that then fails — investigate the lock holder.

## SSO and identity

- `DefaultClaimHandler Subject claim could not be found amongst locally mapped unfiltered remote claims` once per
  tenant-federated login in the multi-tenant SSO block: the first federated pass maps only `groups`; a later pass after
  JIT provisioning resolves the username (verified at runtime against the documented setup; mapping `sub` → username
  does not remove it). *Stops being benign*: the session principal or the API provider is not the expected username.
- `OAuthOpaqueAuthenticatorImpl Invalid OAuth Token ... ACTIVE access token is not found` / `Provided access token is
  invalid` during logout phases where the scenario then asserts re-authentication is required.

## Platform gateway

- Controller WARN `Encryption key file is world-readable` (bind-mounted generated key with default permissions).
- Gateway connection-loss lines at teardown when the control plane stops first (teardown-order noise; see E5).
