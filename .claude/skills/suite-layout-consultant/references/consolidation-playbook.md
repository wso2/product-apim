# Consolidation playbook — joining blocks and merging overlays safely

## What consolidation buys

Every block removed saves a full boot (~2–3 min of lane time) and a container's CPU/memory share. Every runner that
can live in an existing block instead of its own avoids that cost permanently.

## The worked example: WsProxy and NetworkAccessControl

Seven WebSocket-proxy runners and several outbound network-access-control runners each needed one small, distinct
server configuration (a `[[transport.ws.proxy_profile]]` entry, an access-control mode). Instead of a block each, every
runner moved into a block whose own overlay is orthogonal to it, and its few keys were added to that block's overlay:

| Runner | Host block (overlay) |
|---|---|
| GatewayWebSocketProxyAuthRoutingRunner, NetworkAccessControlAllowRunner, NetworkAccessControlUrlAllowRunner | ApprovalWorkflow (`approveWorkflowAllInOne`) |
| GatewayWebSocketProxyAnonRoutingRunner, NetworkAccessControlTenantConfRunner | LoggingAndElkAnalytics (`loggingElkAnalytics`) |
| GatewayWebSocketProxyAuthWrongCredsRunner, NetworkAccessControlUrlTenantAllowRunner | CrossTenantSubscription (`crossTenantSubAllInOne`) |
| GatewayWebSocketProxySpecificBypassRunner | BackendJwtUrlSafeAndMtlsChain (`backendJwtUrlSafeMtlsChain`) |
| GatewayWebSocketProxyCatchAllRunner | SecurityAuditCorsDisabledAndSecretRotation (`is7txSecurityAudit`) |
| GatewayWebSocketProxyNoMatchRunner | Solace (`solace`) |
| GatewayWebSocketProxySpecificOverCatchAllRunner | NewVersionNotification (`newVersionNotificationWsProxySpecificOverCatchAll`) |
| NetworkAccessControl*PrivateBlockRunner ×4 | kept together in NetworkAccessControlPrivateBlock (they share one policy config) |

The rule they follow: **one host per conflicting config** (proxy profiles that would override each other go to different
blocks), **many runners per compatible config** (the four private-block runners share one block).

## Compatibility checklist (all must hold; record the evidence)

1. **Overlay orthogonality.** The two overlays touch different TOML tables/keys, or identical values. Watch for:
   - TOML **arrays** (`[[event_listener]]`, `[[transport.ws.proxy_profile]]`, …) — an overlay array *replaces* the
     distribution's array; merging two overlays' arrays must keep every entry both need (and any distribution default
     entry, e.g. the `token_revocation` event listener).
   - "Loud" keys that change behaviour for everyone in the container: `[apim.oauth_config] auth_header`,
     `[apim.cors] enable`, gateway token cache, `[oauth] allowed_scopes`, unlimited tier disabled, network access
     control modes, analytics enable. A runner whose flows depend on the default must not join such a block unless
     verified (run it there).
   - j2 template overlays, `serverFilesToCopy` (jars, sequences), required application attributes.
2. **The block's subject is not weakened.** Read the host block's suite comment and runner javadoc for "keep
   separate" reasons; verify the claim (comments can be stale — a mutation run settles it). A merge must not make a test
   pass trivially (e.g. turning a cache off that a revocation test relies on).
3. **Shared features.** List every runner that includes the feature files involved, and every block those runners sit
   in; a scenario that moves its config affects all of them.
4. **Server-global state and restarts.** A runner that restarts the server or flips global config (workflow executors,
   log4j properties, tenant config) needs `thread-count="1"` and an ordering slot (`keep X first/last`); it can still
   share the block with runners that tolerate it running sequentially.
5. **Infrastructure parameters.** The host block must provide what the runner needs (`initBackend`, `initProxy`,
   `initTenantUsers`, `initSecondaryUserStore`, `bootExternalIdentityServer`, `emailUserMode`, `lifecycleLock`); adding a
   parameter is fine when cheap (seconds), costly when it boots another container (IS, Squid).
6. **Fixed aliases and locks.** External-IS blocks using the APIM notification alias serialise; browser blocks share a
   lifecycle lock. Do not add a runner that needs a lock to a block that would then hold it far longer.
7. **Isolation inside the block.** With `thread-count` > 1, runners must not collide on names (unique `${UNIQUE}`),
   tenant-global config, or shared actors' mutable state.

## After merging

Rename blocks/overlays whose names no longer describe their contents only when cheap; otherwise update the suite
comment so the next reader knows why the runners are together. Run the merged block, then a 4-block focused run with
the heaviest neighbours, and confirm both runners still prove what they claim (block-log-audit).
