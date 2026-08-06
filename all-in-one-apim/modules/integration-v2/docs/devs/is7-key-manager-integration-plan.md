# WSO2 IS 7.x Third-Party Key Manager — integration-v2 Design & Coverage

Integration-v2 coverage for **WSO2 Identity Server 7.x as a third-party Key Manager** (control-plane
config + runtime token flows). Before this, the suite had only control-plane KM CRUD for IS 6.x-style
connector types with endpoints stored-not-contacted; the `WSO2-IS-7` connector type (DCR v1.1, `scim2/Me`
UserInfo, API-Resource-Management endpoint, `scim2/v2/Roles`, roles creation, Basic vs Mutual-TLS client
auth, tenant sharing, token-revocation event listener, `system_primary_<role>` naming) and the entire
external-KM data plane (DCR, token issuance, gateway validation, revocation, token exchange) were untested.

## Architecture

### Infrastructure vs product behaviour
- **`bootExternalIdentityServer`** (block param, `BlockLifecycleListener`) provisions IS *infrastructure*
  only: starts the `IdentityServerContainer` (`wso2/wso2is:7.3.0`, shared docker network, fixed alias
  `wso2is`), augments APIM's client-truststore BEFORE boot (the JVM reads it once), swaps APIM's TLS
  keystore to `wso2am.p12`, awaits IS OIDC discovery, and publishes the host-mapped IS base URL to the
  block's shared scope as `isBaseUrl`.
- **Registering IS as a key manager is admin product behaviour, done by the features** — inline where
  registration is the subject (the end-to-end token-flow outline, the introspection/PEM variants), or in a
  `_setup_*` fixture where it is a prerequisite (`_setup_is7_key_manager`, `_setup_is7_grant_app`,
  `_setup_is7_role_enforcement`). The listener registers nothing.
- Runtime KM payloads (`wso2is7.json`, `-mtls`, `-introspect`, `-pem`) use `${UNIQUE:…}` names; keygen
  payloads reference the created KM via `{{…Name}}` context refs (the create step stores id + name).

### Concurrency model
- All IS7 blocks share ONE `IdentityServerContainer` (lazy singleton keyed by IS toml overlay; distinct
  overlays cannot coexist in a JVM — both would claim the `wso2is` alias) and run concurrently under the
  suite's `parallel="tests"`.
- The IS→APIM reverse channel (token-revocation notify to `https://wso2am:9443/internal/data/v1/notify`)
  needs APIM to hold the fixed `wso2am` alias. Only blocks whose tests assert on a delivered notification
  set **`receiveExternalIsNotifications=true`**; holders serialize on a JVM-wide permit in
  `BlockLifecycleListener` (at most one live alias holder) while alias-free blocks run freely.
- Blocks registering **same-issuer KMs** (the issuer is IS-global) must be `thread-count=1`: the gateway
  resolves a token's KM by issuer, so two live same-issuer KMs make validation-mode resolution
  nondeterministic.

### Suite layout (`testng-v2.xml`)
- **`IntegrationV2-Is7KeyManager`** (product section, after Admin; overlay = `wso2am.p12` keystore only —
  pure KM plumbing): end-to-end token-flow outline (BasicAuth AND Mutual-TLS client-auth rows), well-known
  discovery, KM coexistence/permissions/grant-handling extras, IS-side role creation, grant types + keygen
  negatives + token validation, tenant-org revocation, role-based enforcement. Holds the notification
  alias. `thread-count=1` (the runners mutate the tenant-global KM registry — see pinned behaviours).
- **Config-overlay section**: `Is7KeyManagerValidationModes` (introspection + PEM merged; the overlay keys
  are test-convergence pins verified mutually inert; tc=1 per same-issuer rule), `Is7TokenExchange`
  (`enforce_type_header_validation=true` is behaviour under test), `Is7DefaultKeyManager` and
  `Is7TenantSharingOff` (IS-free direct-notify blocks), and the token-exchange-disabled negative co-hosted
  in `SandboxAndTokenExchangeDisabled` (it has no gateway/API surface).

### Tagging
Product tags `@cap:admin @feat:external-key-manager` (feat id in `capability-map.yml`), per-area `@rule`s,
`@type`/`@dep:gateway` per scenario; features live in `features/admin/`. 38 scenarios render in the
coverage tree under Admin → external-key-manager.

## KM registration payload (the `WSO2-IS-7` connector, exact)
- `type: "WSO2-IS-7"`; endpoints on the network alias (issuer/token/introspect/revoke/authorize/scope, DCR
  `https://wso2is:9443/api/identity/oauth2/dcr/v1.1/register`, userInfo `…/scim2/Me`).
- `certificates: {type: JWKS, value: https://wso2is:9443/oauth2/jwks}` — or `type: PEM` with the base64 of
  IS's TOKEN-SIGNING cert (`artifacts/certs/is7signing/is7-signing.pem`, pinned to the wso2is:7.3.0 image;
  the TLS cert is a different key pair and would NOT validate tokens).
- **Mandatory IS7-specific `additionalProperties`** (missing → `901401`): `api_resource_management_endpoint`
  (`…/api/server/v1/api-resources`), `is7_roles_endpoint` (`…/scim2/v2/Roles`), `Username`/`Password`
  (BasicAuth) and `self_validate_jwt`.
- **The real self-validate switch is top-level `enableSelfValidationJWT`** — the
  `additionalProperties.self_validate_jwt` mirror is ignored for the gateway's mode.
- Mutual-TLS variant (`wso2is7-mtls.json`): connector client auth `tls_client_auth`; the DCR/keygen leg is
  what exercises APIM's client certificate against IS. IS 7.3.0 requires explicit
  `[client_certificate_based_authentication]` enablement + a trusted-issuer/thumbprint/user mapping in the
  IS toml (7.1.0 accepted client certs without it — behavioural change; config shipped in
  `tests-common/.../is7/deployment-overlay.toml`, values bound to the fixed wso2am cert).
- KM `description` max 256 chars (400 otherwise).

## Certificate & trust recipe
- Each server presents a TLS cert whose **CN + SAN match its network alias** (`wso2is` / `wso2am`); the
  PRIMARY keystores stay untouched so token signing/JWKS and existing OAuth clients survive
  (`[keystore.tls]` only; PKCS12 on IS 7.x, JKS on APIM).
- **Cross-trust** each cert into the other server's client-truststore. **Self-trust gotcha:** a server must
  also trust its OWN new TLS cert, or internal SSL (gateway heartbeat, data-bridge `:9711`) fails and APIs
  don't deploy (`liveGatewayCount=0`).
- One-way trust (APIM→IS) suffices for issue+invoke; the reverse (IS→APIM) is only for the revocation
  notify. Build-time artifacts (augmented truststores, `wso2am.p12`, extracted IS toml) are staged under
  `tests-common/testcontainers/target/is7/` by the module's pre-integration-test execs.

## Revocation pipeline (settled by isolated wire-level probes)
- **Self-validate mode**: revoke at IS → the IS event handler jar POSTs `/internal/data/v1/notify` with
  header `X-WSO2-KEY-MANAGER: WSO2-IS` (the handler TYPE the connector registers — the KM instance name
  silently no-ops, APIM's handler lookup is type-keyed with no fallback and still returns 200) → the
  gateway revoked-JWT map, keyed by the token's **jti**. Works with the doc's listener
  `type=AbstractIdentityHandler` and DEFAULT persistent tokens; `OAuthEventInterceptor` as the type renders
  cleanly but NEVER fires (the enabled-lookup is hardcoded to the `AbstractIdentityHandler` class name).
- **Introspection mode**: enforcement converges via the dedicated **IntrospectionCache** (keymgt layer,
  consulted only for expiry, NOT invalidated by the notify pipeline; TTL = `apim.cache.token_expiry_time`,
  default 900s) — the block pins 15s so revoke→401 lands once the entry evicts. It is independent of the
  gateway/KM token caches.
- **Cross-tenant**: the IS notification carries `tenantDomain=carbon.super` (all KM-DCR'd apps live in IS's
  super tenant), yet a tenant-org gateway context still enforces the revocation — the revoked-JWT lookup is
  token-keyed, not tenantDomain-keyed. No enforcement gap (pinned by the tenant revocation scenario).
- **PEM mode**: a certificate change has NO cache override (unlike revocation), and KM updates propagate
  asynchronously — the rotation walk requires `[apim.cache.gateway_token] enable=false` so fresh-token
  polls re-validate and observe the 200→401→200 flips.

## Token exchange (RFC 8693)
- Subject-token trust is NOT the KM registration: it is a **trusted IdP on APIM's embedded IS** (SOAP
  `addIdP` — APIM has no REST IdP API), resolved by metadata property `idpIssuerName` = the subject token's
  `iss`; the IdP `alias` must equal a value in the token's `aud` (IS client-credentials tokens carry
  `aud=client_id`, so alias = the IS app's client id). PEM = the IdP `<certificate>` (IS JWKS `x5c[0]`);
  JWKS = the `jwksUri` property. `addIdP` validates certificates at registration (wrong format → refused,
  never a hang).
- App keys come from the **Resident KM** with the token-exchange grant; the exchanged token is an `at+jwt`
  APIM token. The block runs `enforce_type_header_validation=true`, pinning that an id_token (no `at+jwt`
  typ) is refused at the gateway while the exchanged token passes.
- Grant-disabled negative: the token endpoint returns `invalid_request` / "Unsupported grant_type value" —
  NOT `unsupported_grant_type`. Rotation canary: a PEM IdP pinned to a stale key pair rejects a live
  subject token while JWKS re-fetches and accepts; EXCHANGED-type KMs register a trusted issuer keyed by
  ISSUER (two same-issuer EXCHANGED KMs conflict; KM delete removes the IdP).

## Tenant sharing (direct-notify blocks, no live IS needed)
The WSO2-IS-7 default KM is materialized FROM the `tenantCreated` notify event, not fetched from IS —
`identity_server_base_url` may be unreachable. With `[[apim.tenant_sharing]]
auto_configure_key_manager=true` + `skip_create_resident_key_manager=true`, a notify-provisioned tenant
gets exactly one KM of type `WSO2-IS-7`; without the tenant-sharing block, it gets NONE and keygen is
refused (`901403`) — pinning the documented dependency. The notify endpoint accepts the event and creates
the tenant regardless; the failure surfaces only at key generation.

## Pinned behaviours (empirical; block comments carry these where they constrain topology)
- **`900967`** — API/shared-scope create fans scope registration synchronously to ALL tenant KMs; a
  transient unreachable/disabled KM (the keygen negatives) makes concurrent sibling creates 500. Core block
  is tc=1 for this.
- **KM-holder propagation races** — a freshly-registered KM reaches the in-memory holder asynchronously:
  (a) a shared scope created pre-propagation gets 201 but its IS role is never created → the scope-create
  step poll-and-recreates until the role appears; (b) a keygen in the window fails "Key Manager not
  configured" AFTER inserting the key-mapping row, which is never rolled back — the leaked row turns every
  retry into `901409 Key Mappings already exists` (product-bug candidate; the raw client's general-error
  retry masks the 500) → the `…and wait until it is operational` create-KM step variant probes with a
  throwaway app + keygen, deleting the probe app (and any leaked row) each attempt.
- **`901411`** — `skip_create_resident_key_manager` breaks API CREATION container-wide, not just keygen:
  no API-creating runner can share the default-KM/tenant-sharing-off containers.
- **`903015`** — a required custom API property rejects lifecycle PUBLISH of any property-less API (and
  empty-value updates); creates pass. (Known-but-untested surface: a publish-gate scenario is a candidate
  addition to the mandatory-properties feature.)
- **KM permissions resolve by KM UUID** — a keygen payload referencing a permission-restricted KM by NAME
  skips the DENY check silently; the DENY scenario references by `{{denyKm}}` id.
- **`availableGrantTypes` is not enforced at keygen** — requesting an unlisted grant is accepted and echoed.
- **IS 7.3.0 discovery grant set**: client_credentials, password, refresh_token, authorization_code,
  jwt-bearer, device_code, token-exchange, saml2-bearer, iwa:ntlm. `implicit` is NOT supported; iwa:ntlm is
  parked (not headless-feasible). PKCE verifier must be ≥43 chars (RFC 7636). SAML assertions are
  back-dated 60s (VM clock skew).
- **Well-known import gotcha** (connector doc, still current): discovery auto-populates UserInfo as
  `oauth2/userinfo`; the working endpoint is `scim2/Me` — pinned by the discovery feature as a regression
  canary.

## Parked / follow-ups
- **Multi-tenant SSO lane** (`testng-is7sso.xml`, `MultiTenantSsoRunner`/`Steps`/`Provisioner`, `is7sso/`
  overlays): kept out of this PR, locally staged for a follow-up. It boots a second IS with a distinct toml
  overlay (tenant-sync listener), which would collide with the default IS singleton on the `wso2is` alias —
  parallel distinct overlays need per-overlay aliases/networks + payload templating. Its login scenario
  currently pins the token-exchange double-client-auth regression rather than a successful login.
- Product-bug reports: the keygen mapping-row leak (`901409` mask), the scope fan-out create abort
  (`900967`), and the IS 7.3.0 management-API client-cert auth change (config workaround shipped).
- `903015` publish-gate scenario for the mandatory-properties feature.

## References
- docs-apim: `api-security/key-management/third-party-key-managers/overview.md` and
  `configure-wso2is7-connector.md`
- RFC 8693 (token exchange), RFC 9068 (`at+jwt`), RFC 7636 (PKCE)
- Existing control-plane coverage extended (not duplicated): `admin/key_manager_config.feature`
