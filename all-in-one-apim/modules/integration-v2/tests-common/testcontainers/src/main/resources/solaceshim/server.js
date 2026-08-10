#!/usr/bin/env node
/*
 * Standalone mock of the Solace Cloud / Event Portal API that WSO2 APIM's v2 Solace client calls.
 * Zero dependencies -- pure node:http. Run it, point APIM at it, watch the log.
 *
 *   node solace-mock.js                 # listens on 9081
 *   PORT=9090 node solace-mock.js
 *
 * Then in APIM's deployment.toml:
 *
 *   [apim.solace_config]
 *   apim_api_endpoint = "http://localhost:9081"
 *   token = "any-non-empty-string"
 *
 * ROUTES -- taken from the Feign interface SolaceV2ApimApisClient (apimgt.solace 9.33.162), not guessed:
 *   GET    /eventApiProducts
 *   GET    /eventApiProducts/{productId}/plans
 *   GET    /eventApiProducts/{productId}/plans/{planId}/eventApis/{eventApiId}   (APIM adds ?asyncApiVersion=2.2.0)
 *   POST   /appRegistrations
 *   GET    /appRegistrations/{registrationId}
 *   DELETE /appRegistrations/{registrationId}
 *   POST   /appRegistrations/{registrationId}/credentials
 *   POST   /appRegistrations/{registrationId}/accessRequests
 *   GET    /appRegistrations/{registrationId}/accessRequests
 *   DELETE /appRegistrations/{registrationId}/accessRequests/{accessRequestId}
 *
 * ENVELOPE RULE -- differs per endpoint, and getting it backwards fails silently:
 *   - /eventApiProducts and .../plans are wrapped in {"data": [...]}  (SolaceV2Apis reads .data)
 *   - the eventApis definition is returned BARE: SolaceV2Apis.getEventApiAsyncApiDefinition does
 *     `return client.getEventApiAsyncApiDefinition(...)` with no unwrapping, and APIM hands that
 *     object straight to the Publisher UI, which uploads it as the import-asyncapi file.
 *
 * AUTH: the Authorization header is LOGGED but never rejected, deliberately -- a 401 from here would
 * look like a config problem in APIM and send you down the wrong path. Check the log to confirm the
 * token from [apim.solace_config] is actually arriving.
 */
'use strict';
const http = require('node:http');

const PORT = Number(process.env.PORT || 9081);

// ---------------------------------------------------------------------------
// FIXTURES -- edit these freely; ids are referenced across the three GETs.
// ---------------------------------------------------------------------------
const PRODUCT_ID = '8xk2p9qz1a';
const PLAN_ID = 'pl4n7bq2xd';
const EVENT_API_ID = 'api5t8w2rk';

const APPLICATION_DOMAIN_ID = '3mfd8k2l9c';

/*
 * MEASURED: the Solace plan is resolved BY NAME against the APIM SUBSCRIPTION TIER --
 * SolaceSubscriptionsNotifier passes SubscriptionEvent.getPolicyId() to
 * SolaceV2Apis.getEventApiProductPlanId(productId, planName), which fetches /plans and matches on `name`.
 * A miss fails with "Cannot find a Solace Plan with the name: '<tier>' in the Solace Event API Product".
 * So the plan names here must cover whatever tier you subscribe with. The async tiers APIM ships are
 * listed below; add yours if you use a custom tier.
 */
const PLAN = {
    id: PLAN_ID,
    name: 'AsyncUnlimited',
    solaceClassOfServicePolicy: { id: 'cos9v3k1mz', messageDeliveryMode: 'GUARANTEED' },
};

const PLANS = [
    PLAN,
    { id: 'pl4nGold01', name: 'AsyncGold',   solaceClassOfServicePolicy: { id: 'cosGold01', messageDeliveryMode: 'GUARANTEED' } },
    { id: 'pl4nSilv01', name: 'AsyncSilver', solaceClassOfServicePolicy: { id: 'cosSilv01', messageDeliveryMode: 'GUARANTEED' } },
    { id: 'pl4nBrnz01', name: 'AsyncBronze', solaceClassOfServicePolicy: { id: 'cosBrnz01', messageDeliveryMode: 'DIRECT' } },
    { id: 'pl4nGoldXX', name: 'Gold',        solaceClassOfServicePolicy: { id: 'cos9v3k1mz', messageDeliveryMode: 'GUARANTEED' } },
];

const EVENT_API_PRODUCT = {
    id: PRODUCT_ID,
    name: 'SolaceEventApi-1.0.0',
    displayName: 'Solace Event API',
    description: 'Order events exposed as an Event API Product',
    version: '1.0.0',
    state: 'RELEASED',
    brokerType: 'solace',
    approvalType: 'AUTOMATIC',
    applicationDomainId: APPLICATION_DOMAIN_ID,
    applicationDomainName: 'TestWSO2',
    createdTime: 1767225600000,      // epoch MILLIS -- the model field is a long, not an ISO string
    updatedTime: 1767225600000,
    createdBy: 'solaceadmin',
    changedBy: 'solaceadmin',
    plans: PLANS,
    solaceMessagingServices: [{
        id: 'sms1a7c4te',
        messagingServiceId: 'msvc6h2n8p',
        messagingServiceName: 'devPortTestEnv-broker',
        environmentName: 'devPortTestEnv',
        eventMeshName: 'TestWSO2-mesh',
        supportedProtocols: ['smf', 'smfs', 'mqtt', 'amqp', 'rest'],
    }],
    apis: [{
        id: EVENT_API_ID,
        name: 'SolaceEventApi',
        displayName: 'Solace Event API',
        description: 'Order events',
        version: '1.0.0',
        customAttributes: [],
    }],
    customAttributes: [],
    attributes: [],
    apiParameters: [],
};

// Returned BARE (no data envelope) -- this is what the Publisher UI uploads to import-asyncapi.
const ASYNC_API_DEFINITION = {
    asyncapi: '2.2.0',
    /*
     * These four x-ep-* extensions are REQUIRED and live under `info`. MEASURED the hard way: without
     * them, subscribing to the imported API dies with
     *   NullPointerException: Cannot invoke "JsonElement.getAsString()" because the return value of
     *   "JsonObject.get(String)" is null
     *     at SolaceSubscriptionsNotifier.createAccessRequest(SolaceSubscriptionsNotifier.java:89)
     * and the devportal shows a bare 500. The notifier reads them off the API's STORED AsyncAPI
     * definition (INFO_OBJECT_KEY = "info") to work out which Event API Product the subscription maps
     * to -- real Event Portal embeds them, so nothing in the import path validates their presence.
     * NOTE: they are read at SUBSCRIBE time from the stored definition, so adding them here only helps
     * APIs imported AFTERWARDS -- an API already imported without them must be re-imported.
     */
    info: {
        title: 'Solace Event API',
        version: '1.0.0',
        description: 'Order events exposed via Solace Event Portal',
        // The four APIM actually reads (SolaceSubscriptionsNotifier).
        'x-ep-event-api-product-id': PRODUCT_ID,
        'x-ep-event-api-product-name': 'SolaceEventApi-1.0.0',
        'x-ep-event-api-product-version': '1.0.0',
        'x-ep-application-domain-id': APPLICATION_DOMAIN_ID,
        // Present in real Event Portal output (per Solace's APIM/DevPortal API walkthrough) but NOT read
        // by APIM. Included so the fixture resembles a genuine response rather than the minimum APIM
        // tolerates -- a mock trimmed to exactly what the consumer reads stops being a useful reference.
        'x-ep-event-api-id': EVENT_API_ID,
        'x-ep-event-api-version': '1.0.0',
        'x-ep-state-name': 'RELEASED',
        'x-ep-shared': 'true',
    },
    servers: {
        production: {
            url: 'solacebroker:55555',
            protocol: 'smf',
            description: 'Solace PubSub+ SMF endpoint',
            bindings: { solace: { bindingVersion: '0.3.0', msgVpn: 'default' } },
        },
    },
    /*
     * Both operations on the one channel, deliberately. AsyncAPI 2.x states direction FROM THE APPLICATION'S
     * point of view: `publish` = the app may send to this channel, `subscribe` = the app may receive from it.
     * The access request below derives the broker ACL from exactly these, so declaring both is what grants the
     * application both directions on orders/created -- which is what the invoke scenarios exercise (a REST
     * publish and an MQTT subscribe). Drop one operation and the corresponding half stops being authorised.
     */
    channels: {
        'orders/created': {
            publish: {
                operationId: 'sendOrderCreated',
                message: { $ref: '#/components/messages/OrderCreated' },
            },
            subscribe: {
                operationId: 'onOrderCreated',
                message: { $ref: '#/components/messages/OrderCreated' },
            },
        },
    },
    components: {
        messages: {
            OrderCreated: {
                name: 'OrderCreated',
                contentType: 'application/json',
                payload: { $ref: '#/components/schemas/Order' },
            },
        },
        schemas: {
            Order: {
                type: 'object',
                properties: { orderId: { type: 'string' }, amount: { type: 'number' } },
            },
        },
    },
};

// ---------------------------------------------------------------------------
// Mutable state, so create -> get -> delete behaves like a real service.
// ---------------------------------------------------------------------------
const state = { registrations: {} };
let seq = 0;
const nextId = (p) => `${p}${(++seq).toString().padStart(4, '0')}`;

const log = (...a) => console.log('[solace-mock]', ...a);

// ---------------------------------------------------------------------------
// SEMP forwarding -- what makes the REAL broker load-bearing rather than decoration.
//
// In production the Solace connector does two jobs: it answers APIM's control-plane calls AND provisions
// the broker so the application can actually connect. A double that only does the first leaves the broker
// ignorant of every credential APIM issues, so no invocation can ever be tested and the broker container
// is pure cost. So credentials pushed by APIM are turned into a real SEMP clientUsername here, and removed
// again on delete.
//
// BEST-EFFORT BY DESIGN: every SEMP call is wrapped, and a failure is LOGGED but never fails the
// control-plane response. Two reasons. (1) This file is also run standalone on a laptop with no broker at
// all -- SEMP is simply absent then, and the control-plane flows must still work. (2) A SEMP hiccup must
// not masquerade as an APIM defect; the whole point of the mock is that APIM's behaviour is what's under
// test. Grep the "semp:" lines to see what was actually provisioned.
// Verified against solace/solace-pubsub-standard (SolOS 10.26) before use:
//   POST   /SEMP/v2/config/msgVpns/default/clientUsernames        -> 200
//   GET    /SEMP/v2/config/msgVpns/default/clientUsernames/{name} -> 200, and 400 once deleted
//   DELETE /SEMP/v2/config/msgVpns/default/clientUsernames/{name} -> 200
//   POST   /SEMP/v2/config/msgVpns/default/authenticationOauthProfiles        -> 200
//   PATCH  /SEMP/v2/config/msgVpns/default/authenticationOauthProfiles/{name} -> 200
//   PATCH  /SEMP/v2/config/msgVpns/default                                    -> 200
// ---------------------------------------------------------------------------
const SEMP_URL = process.env.SEMP_URL || '';
const SEMP_USER = process.env.SEMP_USER || 'admin';
const SEMP_PASSWORD = process.env.SEMP_PASSWORD || 'admin';
const SEMP_VPN = process.env.SEMP_VPN || 'default';

async function semp(method, path, body) {
    if (!SEMP_URL) { log(`     semp: SKIPPED (no SEMP_URL; running without a broker) ${method} ${path}`); return null; }
    const url = `${SEMP_URL}/SEMP/v2/config/msgVpns/${SEMP_VPN}${path}`;
    const auth = Buffer.from(`${SEMP_USER}:${SEMP_PASSWORD}`).toString('base64');
    try {
        const res = await fetch(url, {
            method,
            headers: { Authorization: `Basic ${auth}`, 'Content-Type': 'application/json' },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
        const text = await res.text();
        log(`     semp: ${method} ${path} -> ${res.status}${res.ok ? '' : ' ' + text.slice(0, 180)}`);
        return res.status;
    } catch (e) {
        // Never rethrow: see BEST-EFFORT above.
        log(`     semp: ${method} ${path} -> FAILED ${e.message}`);
        return null;
    }
}

/*
 * ---------------------------------------------------------------------------
 * Make the broker accept APIM'S OWN ACCESS TOKENS -- the scheme the product actually drives.
 *
 * WHY NOT A PASSWORD. APIM never issues a broker password. Solace's own "invoke your event API" walkthrough
 * has the client authenticate with the OAuth token the API-management system issued, and APIM's Solace
 * integration pushes an application's consumerKey/consumerSecret -- an OAuth CLIENT credential, not a
 * messaging password. So the broker has to be configured as an OAuth RESOURCE SERVER that trusts APIM as
 * the authorization server, and it validates the token by fetching APIM's JWKS. An earlier version of this
 * file provisioned the consumerKey/consumerSecret as a clientUsername + PASSWORD and switched the VPN to
 * `authenticationBasicType: internal`. That is a scheme the product never uses; it was removed.
 *
 * EVERY LINE BELOW IS MEASURED, and four of these settings are non-obvious enough that guessing them
 * produces a 403 with no clue in the HTTP response (the reason appears ONLY in the broker's
 * /usr/sw/jail/logs/event.log, as SYSTEM_CLIENT_CONNECT_AUTH_FAIL -- look there first, always):
 *
 *   oauthRole: 'resource-server'   The default is 'client' (the broker as an OIDC *client*, for SEMP login).
 *                                  Validating a token a messaging client presents is the resource-server role.
 *
 *   issuer: <APIM's iss>           THE ONE THAT COSTS AN AFTERNOON. The broker picks which profile validates a
 *                                  token by MATCHING THE TOKEN'S `iss` CLAIM against each active profile's
 *                                  `issuer`; `authenticationOauthDefaultProfileName` applies ONLY to a token
 *                                  with no `iss` at all. Getting this wrong fails with "The token issuer
 *                                  ('iss' claim) did not match the issuer of any active profile", and NO
 *                                  amount of relaxing the resourceServerValidate* flags helps, because those
 *                                  govern validation AFTER a profile is selected, not selection itself.
 *
 *   usernameClaimName: 'client_id' Default is 'sub'. APIM's `sub` is an opaque USER uuid, the same for every
 *                                  application; `client_id` (== `azp` == the application's consumerKey) is
 *                                  what identifies the app. Combined with disabling the fallback `default`
 *                                  clientUsername below, this is what makes the credential APIM pushed
 *                                  LOAD-BEARING -- see the mutation proof there.
 *
 *   authorizationGroupsClaimName:''Default is 'groups'. APIM's token has no `groups` claim, so authorization
 *                                  fails AFTER successful authentication with the easily-misread "No
 *                                  Authorization Groups were retrieved from the Authorizing Server". Clearing
 *                                  it falls back to the VPN's internal authorization (the client username's
 *                                  ACL profile). The alternative -- pointing it at `scope` and creating a
 *                                  matching authorizationGroup -- also works but pins Solace config to
 *                                  APIM's scope VALUE ("default"), which is not ours to depend on.
 *
 *   resourceServerValidate{Audience,Scope,Issuer}Enabled: false
 *                                  Each of these validates the token against a `resourceServerRequired*`
 *                                  value that is EMPTY and cannot be filled in here: APIM's `aud` is the
 *                                  per-application consumerKey, not a fixed broker audience. Left enabled,
 *                                  the token is rejected.
 *
 *   resourceServerValidateTypeEnabled is deliberately NOT touched: the broker's default required type is
 *                                  `at+jwt` and APIM emits exactly that, so the shipped default already
 *                                  passes. Verified, rather than assumed and disabled "to be safe".
 *
 * ORDER AND ONE-SHOT-NESS MATTER. The profile is created DISABLED, configured, then enabled, so it is never
 * briefly active with a half-written config. And this runs exactly ONCE at startup: MEASURED that patching a
 * live profile DISCARDS THE CACHED JWKS ("No JSON Web Key exists to decode the token" on the next publish),
 * so reconfiguring mid-run would break invocations that were working. Do not make this lazy or repeated.
 *
 * The JWKS is fetched over PLAIN HTTP (APIM's 9763), not 9443: the broker would otherwise have to trust
 * APIM's self-signed certificate, which means provisioning a certificate authority into the broker. Verified
 * that the broker container can reach APIM's /oauth2/jwks on 9763 and that its `kid` matches the token's.
 * ---------------------------------------------------------------------------
 */
const OAUTH_PROFILE = process.env.OAUTH_PROFILE || 'wso2apim';
const APIM_JWKS_URL = process.env.APIM_JWKS_URL || '';
const APIM_TOKEN_ISSUER = process.env.APIM_TOKEN_ISSUER || 'https://localhost:9443/oauth2/token';

async function configureApimOAuth() {
    if (!SEMP_URL) { log('     semp: SKIPPED OAuth config (no SEMP_URL; running without a broker)'); return; }
    if (!APIM_JWKS_URL) { log('     semp: SKIPPED OAuth config (no APIM_JWKS_URL set)'); return; }
    const profile = `/authenticationOauthProfiles/${encodeURIComponent(OAUTH_PROFILE)}`;

    // 400 here just means the profile survived a restart; the PATCH below is what makes it correct either way.
    await semp('POST', '/authenticationOauthProfiles', { oauthProfileName: OAUTH_PROFILE, enabled: false });
    const configured = await semp('PATCH', profile, {
        oauthRole: 'resource-server',
        endpointJwks: APIM_JWKS_URL,
        issuer: APIM_TOKEN_ISSUER,
        usernameClaimName: 'client_id',
        authorizationGroupsClaimName: '',
        resourceServerValidateAudienceEnabled: false,
        resourceServerValidateScopeEnabled: false,
        resourceServerValidateIssuerEnabled: false,
    });
    const enabled = await semp('PATCH', profile, { enabled: true });
    const vpn = await semp('PATCH', '', {
        authenticationOauthEnabled: true,
        authenticationOauthDefaultProfileName: OAUTH_PROFILE,
        // Basic auth OFF. Not tidiness: the VPN ships `authenticationBasicType: "none"`, under which the
        // broker accepts ANY username/password -- bogus:bogus published successfully (200) on an untouched
        // broker. Leaving it on would make every "bad credential is rejected" assertion VACUOUS, and would
        // also let a bad OAuth token through by being read as a basic password.
        authenticationBasicEnabled: false,
    });
    // Disable the fallback client username. The VPN ships an enabled `default` clientUsername that ANY
    // otherwise-unknown client falls back to -- which is why bogus:bogus connected at all. With it disabled,
    // the username resolved from the token's client_id claim MUST have been provisioned.
    // MEASURED, and this is the mutation proof that provisioning is load-bearing rather than decoration:
    //   no clientUsername for the consumer key -> 403 "Client Username Is Shutdown"
    //   clientUsername provisioned             -> 200
    //   clientUsername deleted again           -> 403 "Client Username Is Shutdown"
    const noFallback = await semp('PATCH', '/clientUsernames/default', { enabled: false });

    if ([configured, enabled, vpn, noFallback].every((s) => s === 200)) {
        log(`     semp: broker configured as OAuth resource server for ${APIM_TOKEN_ISSUER}`);
    } else {
        // Loud, because a partial config fails invocations in a way whose cause is invisible from the test's
        // point of view (a 403 with no detail), and silently voids the negatives.
        log('     semp: WARNING OAuth config INCOMPLETE -- publishes will fail and negatives are unsound');
    }
}

/*
 * Report whether APIM's JWKS is actually FETCHABLE from inside the network, and whether APIM agrees with the
 * issuer we configured. Pure diagnostics -- it changes no config and gates nothing.
 *
 * It exists because the two ways this integration breaks are both INVISIBLE from the test's point of view:
 * a publish just returns 403 with the body "Unauthorized", and the real reason is written only to a file
 * inside the broker container (/usr/sw/jail/logs/event.log), which is gone the moment the block tears down.
 * One suite run was spent discovering that APIM simply had no resolvable network alias. These two lines in
 * the test log would have said so immediately, so they are worth the one request.
 *
 * Run at the first credentials push rather than at startup, because that is the earliest moment APIM is
 * known to be up. The shim is not the broker, but both sit on the same network, so a failure here means the
 * broker's own fetch cannot work either.
 */
let apimProbed = false;
async function probeApimOnce() {
    if (apimProbed || !APIM_JWKS_URL) return;
    apimProbed = true;
    try {
        const res = await fetch(APIM_JWKS_URL);
        log(`     jwks: GET ${APIM_JWKS_URL} -> ${res.status} (THE BROKER must reach this too)`);
    } catch (e) {
        log(`     jwks: GET ${APIM_JWKS_URL} -> UNREACHABLE ${e.message}`);
        log('     jwks: every publish WILL be rejected 403. Is the alias bound (withSolaceJwksAlias)?');
    }
    // APIM's own view of its issuer, which is what its tokens carry and therefore what the broker matches
    // the profile on. Discovery lives beside the JWKS endpoint under /oauth2.
    try {
        const disco = APIM_JWKS_URL.replace(/\/oauth2\/jwks$/, '/oauth2/oidcdiscovery/.well-known/openid-configuration');
        const res = await fetch(disco);
        const iss = res.ok ? (await res.json()).issuer : `(HTTP ${res.status})`;
        const agrees = iss === APIM_TOKEN_ISSUER ? 'matches' : 'MISMATCH -- publishes will be rejected';
        log(`     jwks: APIM reports issuer ${iss}; configured ${APIM_TOKEN_ISSUER} -> ${agrees}`);
    } catch (e) {
        log(`     jwks: could not read APIM's issuer (${e.message}); configured ${APIM_TOKEN_ISSUER}`);
    }
}

/*
 * ---------------------------------------------------------------------------
 * Translate an access request's GRANTED TOPICS into broker ACLs.
 *
 * This is the second half of what a real connector does. Without it the broker authorises ANY topic for an
 * authenticated client, so the invocation scenarios could only ever prove authentication -- an application
 * could publish to topics its subscription never granted and nothing would notice.
 *
 * Shape, MEASURED against SolOS 10.26 before writing it:
 *   POST /aclProfiles {aclProfileName, clientConnectDefaultAction:'allow',
 *                      publishTopicDefaultAction:'disallow', subscribeTopicDefaultAction:'disallow'} -> 200
 *   POST /aclProfiles/{name}/publishTopicExceptions {publishTopicException, publishTopicExceptionSyntax:'smf'}
 *   clientUsername carries `aclProfileName`, set when it is created.
 * Default-DISALLOW plus per-topic exceptions is what makes the grant meaningful; the shipped `default` ACL
 * profile allows everything. clientConnectDefaultAction MUST be 'allow' or the client cannot connect at all,
 * and the failure then looks like an authentication problem rather than an ACL one.
 *
 * NAME LENGTH IS A HARD 32 CHARS -- a hyphenated UUID is 36, so a registration id used verbatim fails with
 * "value must be no greater than 32 characters in length". De-hyphenating a UUID lands on exactly 32.
 *
 * Re-creating a profile or re-adding an exception answers 400 "already exists". That is expected on the
 * credentials path (which ensures the profile exists in case no access request preceded it) and is left to
 * semp()'s best-effort logging rather than being pre-checked with a GET.
 * ---------------------------------------------------------------------------
 */
/*
 * The topics an access request grants, DERIVED from the Event API Product's own AsyncAPI channels rather than
 * hardcoded.
 *
 * Why it matters: these permissions become the broker's ACL profile, so a hardcoded list would authorise
 * orders/created no matter what document the product actually describes -- the enforcement test would then be
 * asserting against a constant, and would keep passing if the channels changed underneath it. Deriving here
 * makes the grant follow the document: rename a channel and the publish is denied.
 *
 * Direction is AsyncAPI 2.x's, taken from the APPLICATION's point of view (`publish` = the app may send,
 * `subscribe` = the app may receive), which maps one-to-one onto Solace's publish/subscribe topic exceptions.
 *
 * NOTE this reads the document THIS SERVER SERVES. The test suite imports its own copy
 * (artifacts/payloads/solace/solace_asyncapi.yaml), and APIM's access request carries only product/plan ids --
 * no channel information -- so the double cannot learn the topics from the request. The two documents must
 * therefore stay in step; the yaml says so at the top.
 */
function permissionsFromChannels() {
    const publish = [];
    const subscribe = [];
    for (const [topic, channel] of Object.entries(ASYNC_API_DEFINITION.channels || {})) {
        if (channel && channel.publish) publish.push(topic);
        if (channel && channel.subscribe) subscribe.push(topic);
    }
    return { publish, subscribe };
}

function aclProfileNameFor(registrationId) {
    return String(registrationId).replace(/[^A-Za-z0-9_]/g, '').slice(0, 32);
}

async function ensureAclProfile(registrationId, permissions) {
    const name = aclProfileNameFor(registrationId);
    await semp('POST', '/aclProfiles', {
        aclProfileName: name,
        clientConnectDefaultAction: 'allow',
        publishTopicDefaultAction: 'disallow',
        subscribeTopicDefaultAction: 'disallow',
    });
    for (const topic of (permissions && permissions.publish) || []) {
        await semp('POST', `/aclProfiles/${encodeURIComponent(name)}/publishTopicExceptions`,
            { publishTopicException: topic, publishTopicExceptionSyntax: 'smf' });
    }
    for (const topic of (permissions && permissions.subscribe) || []) {
        await semp('POST', `/aclProfiles/${encodeURIComponent(name)}/subscribeTopicExceptions`,
            { subscribeTopicException: topic, subscribeTopicExceptionSyntax: 'smf' });
    }
    return name;
}

/**
 * Provision the application's OAuth client id as a broker client username.
 *
 * This is what the real connector does with the credential APIM pushes, and it is LOAD-BEARING here: the
 * broker resolves a connecting client's username from the token's `client_id` claim (== this consumerKey),
 * and the `default` fallback username is disabled, so a client whose username was never provisioned is
 * rejected. The password is set because a real connector sets one; nothing reads it while basic auth is off.
 */
async function provisionClientUsername(consumerKey, consumerSecret, registrationId) {
    if (!consumerKey) { log('     semp: no consumerKey in credentials body; nothing to provision'); return; }
    await probeApimOnce();
    // The profile normally already exists (the access request created it when the application subscribed);
    // this covers key generation with no preceding subscription, where the application ends up with a profile
    // that grants NOTHING -- which is the correct outcome, not an oversight.
    const aclProfileName = await ensureAclProfile(registrationId, null);
    await semp('POST', '/clientUsernames', {
        clientUsername: consumerKey,
        password: consumerSecret || '',
        aclProfileName,
        enabled: true,
    });
}

async function deprovisionClientUsername(consumerKey) {
    if (!consumerKey) return;
    await semp('DELETE', `/clientUsernames/${encodeURIComponent(consumerKey)}`);
}

/*
 * MEASURED (2026-08-05, APIM 4.7.0-SNAPSHOT): on key generation, APIM posts straight to
 *   POST /appRegistrations/{APIM-application-UUID}/credentials
 * having NEVER called POST /appRegistrations -- observed count of creates was exactly 0, and
 * isAppRegistrationExists()/GET /appRegistrations/{id} was not called either. So the registrationId is
 * the APIM application's own UUID, and the sub-resource arrives before anything creates the parent.
 *
 * A strict mock 404s that and the key-gen leg fails. Whether REAL Solace Cloud creates the registration
 * on demand or also 404s is UNKNOWN and not something this mock can settle -- so it auto-creates and says
 * so in the log, rather than pretending the parent was there all along. If you are chasing an ordering bug
 * in APIM, the AUTO-CREATED lines are the evidence you want.
 */
function ensureRegistration(id, why) {
    if (!state.registrations[id]) {
        log(`     AUTO-CREATED registration ${id} (${why} arrived before any POST /appRegistrations)`);
        state.registrations[id] = {
            registrationId: id,
            source: 'WSO2_APIM',
            name: `auto-${id.slice(0, 8)}`,
            sourceOwner: 'admin',
            applicationDomainId: EVENT_API_PRODUCT.applicationDomainId,
            brokerType: 'solace',
            accessRequests: [],
            credentials: [],
            autoCreated: true,
        };
    }
    return state.registrations[id];
}

function send(res, status, body) {
    const payload = body === undefined ? '' : JSON.stringify(body, null, 2);
    res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) });
    res.end(payload);
    log(`  -> ${status}${payload && payload.length < 260 ? ' ' + payload.replace(/\s+/g, ' ') : ''}`);
}

/*
 * Strip any base prefix the operator configured. Real Solace Cloud lives under
 * /api/v2/architecture, and APIM appends the Feign path to whatever apim_api_endpoint says -- so the
 * incoming path may or may not carry a prefix. Anchoring on the first known segment means the same
 * mock works whichever way apim_api_endpoint is written.
 */
function normalisePath(pathname) {
    const i = pathname.search(/\/(eventApiProducts|appRegistrations|_mock)\b/);
    return i === -1 ? pathname : pathname.slice(i);
}

const server = http.createServer((req, res) => {
    let raw = '';
    req.on('data', (c) => { raw += c; });
    req.on('end', () => {
        const url = new URL(req.url, `http://localhost:${PORT}`);
        const path = normalisePath(url.pathname);
        const auth = req.headers.authorization;

        log(`${req.method} ${url.pathname}${url.search}`);
        log(`     auth: ${auth ? auth.slice(0, 24) + '…' : 'NONE (APIM sent no Authorization header)'}`);
        if (raw) log(`     body: ${raw.slice(0, 500)}`);

        let body = null;
        if (raw) { try { body = JSON.parse(raw); } catch { /* leave null; logged above */ } }

        const seg = path.split('?')[0].split('/').filter(Boolean);
        const m = req.method;

        // ---- GET /eventApiProducts ------------------------------------------------ (data envelope)
        if (m === 'GET' && seg.length === 1 && seg[0] === 'eventApiProducts') {
            return send(res, 200, { data: [EVENT_API_PRODUCT], meta: {} });
        }

        // ---- GET /eventApiProducts/{id}/plans ------------------------------------- (data envelope)
        if (m === 'GET' && seg.length === 3 && seg[0] === 'eventApiProducts' && seg[2] === 'plans') {
            return send(res, 200, { data: PLANS, meta: {} });
        }

        // ---- GET /eventApiProducts/{id}/plans/{planId}/eventApis/{eventApiId} ----- (BARE)
        if (m === 'GET' && seg.length === 6 && seg[0] === 'eventApiProducts'
            && seg[2] === 'plans' && seg[4] === 'eventApis') {
            log(`     (definition for product=${seg[1]} plan=${seg[3]} eventApi=${seg[5]}, `
                + `asyncApiVersion=${url.searchParams.get('asyncApiVersion')})`);
            return send(res, 200, ASYNC_API_DEFINITION);
        }

        // ---- POST /appRegistrations ------------------------------------------- (data-WRAPPED object)
        // MEASURED: SolaceV2Apis.createAppRegistration does
        //     new Gson().fromJson(response.getAsJsonObject("data"), AppRegistration.class)
        // so a BARE registration object makes getAsJsonObject("data") return null, fromJson yield null,
        // and the caller NPE on registrationId. The wrapper is load-bearing.
        if (m === 'POST' && seg.length === 1 && seg[0] === 'appRegistrations') {
            // APIM sends the APIM application UUID as the registration name/id source; prefer whatever it
            // sends so subsequent sub-resource calls (keyed by app UUID) hit the same record.
            const id = (body && (body.registrationId || body.name)) || nextId('reg');
            const reg = {
                registrationId: id,
                // DEFAULT_APP_SOURCE_AND_OWNER in the bundle is the literal "wso2apim"
                source: (body && body.source) || 'wso2apim',
                name: (body && body.name) || `app-${String(id).slice(0, 8)}`,
                sourceOwner: (body && body.sourceOwner) || 'wso2apim',
                applicationDomainId: (body && body.applicationDomainId) || APPLICATION_DOMAIN_ID,
                brokerType: 'solace',
                accessRequests: [],
                credentials: [],
            };
            state.registrations[id] = reg;
            return send(res, 201, { data: reg, meta: {} });
        }

        // ---- GET|DELETE /appRegistrations/{registrationId} -----------------------------------
        if (seg.length === 2 && seg[0] === 'appRegistrations') {
            const reg = state.registrations[seg[1]];
            if (m === 'GET') {
                /*
                 * 404 for absent -- this is the DOCUMENTED Solace behaviour and the path APIM is written
                 * for. isAppRegistrationExists() decompiles to:
                 *     try { JsonObject r = client.getAppRegistration(id); return r != null && r.has("data"); }
                 *     catch (FeignException e) { if (e.status() == 404) return false; throw ...; }
                 * An earlier version of this mock answered 200 {} instead, reasoning that has("data")==false
                 * also yields false. That DOES make APIM work, but it is WRONG as a mock: it bypasses the
                 * FeignException branch entirely, so the code path production actually takes against real
                 * Solace never gets exercised. A mock that dodges the real path hides real bugs.
                 * Solace's APIM/DevPortal API documents 404 among its status codes.
                 */
                if (!reg) {
                    log(`     (no registration ${seg[1]} -> 404, exercising APIM's FeignException/404 branch)`);
                    return send(res, 404, { message: `registration ${seg[1]} not found` });
                }
                return send(res, 200, { data: reg, meta: {} });
            }
            if (m === 'DELETE') {
                // Remove every clientUsername this registration provisioned, then its ACL profile, so the
                // broker does not keep credentials or grants APIM has forgotten -- the residue a real
                // deployment would accumulate. The broker is shared by every scenario in the block, so this
                // is not merely tidiness: leftover profiles would accumulate across a whole suite run.
                // ORDER MATTERS: the profile is still referenced while a clientUsername using it exists.
                const cleanup = reg
                        ? Promise.all(reg.credentials.map(
                                (c) => deprovisionClientUsername(c.secret && c.secret.consumerKey)))
                        : Promise.resolve();
                cleanup.then(() => semp('DELETE', `/aclProfiles/${encodeURIComponent(aclProfileNameFor(seg[1]))}`))
                        .then(() => {
                            delete state.registrations[seg[1]];
                            send(res, 200, { data: { registrationId: seg[1], deleted: true }, meta: {} });
                        })
                        .catch((error) => {
                            log(`     ERROR deleting registration ${seg[1]}: ${error.stack || error}`);
                            send(res, 500, { message: `failed to delete registration ${seg[1]}` });
                        });
                return;
            }
        }

        // ---- POST /appRegistrations/{id}/credentials -----------------------------------------
        if (m === 'POST' && seg.length === 3 && seg[0] === 'appRegistrations' && seg[2] === 'credentials') {
            const reg = ensureRegistration(seg[1], 'credentials');
            const now = 1767225600000;
            const cred = {
                secret: {
                    consumerKey: (body && body.secret && body.secret.consumerKey) || nextId('ck'),
                    consumerSecret: (body && body.secret && body.secret.consumerSecret) || nextId('cs'),
                },
                issuedAt: now,
                expiresAt: now + 3600_000,
            };
            reg.credentials.push(cred);
            /*
             * AWAIT the broker provisioning before answering 201 -- do NOT make this fire-and-forget.
             * MEASURED: an earlier fire-and-forget version returned 201 immediately, and a test that
             * published as soon as key generation returned raced the SEMP call and got 403 from the broker.
             * It looked exactly like a credential defect. Awaiting also matches the real connector, which
             * cannot honestly confirm a credential it has not yet created.
             * Provisioning stays best-effort INTERNALLY (semp() never throws), so a broker-less run still
             * gets its 201 -- awaiting changes the ordering, not the error policy.
             */
            provisionClientUsername(cred.secret.consumerKey, cred.secret.consumerSecret, seg[1])
                    .then(() => send(res, 201, cred))
                    .catch((error) => {
                        log(`     ERROR provisioning credentials for registration ${seg[1]}: ${error.stack || error}`);
                        send(res, 500, { message: `failed to provision credentials for registration ${seg[1]}` });
                    });
            return;
        }

        // ---- POST|GET /appRegistrations/{id}/accessRequests ----------------------------------
        if (seg.length === 3 && seg[0] === 'appRegistrations' && seg[2] === 'accessRequests') {
            const reg = ensureRegistration(seg[1], 'accessRequests');
            if (m === 'POST') {
                const ar = {
                    accessRequestId: nextId('acc'),
                    eventApiProductId: (body && body.eventApiProductId) || PRODUCT_ID,
                    planId: (body && body.planId) || PLAN_ID,
                    registrationId: seg[1],
                    eventApiProductVersion: (body && body.eventApiProductVersion) || '1.0.0',
                    // Real states are APPROVED | AWAITING_APPROVAL | LIVE; a real APPROVED transitions to
                    // LIVE once the broker is configured. APPROVED is returned here because the product's
                    // approvalType is AUTOMATIC; switch to AWAITING_APPROVAL to exercise the manual path.
                    state: 'APPROVED',
                    eventApiProductResourceInformation: {
                        accessType: 'exclusive',
                        maxMsgSpoolUsage: 5000,
                        maxTtl: 3600,
                        name: EVENT_API_PRODUCT.name,
                    },
                    // NOT read by APIM, but no longer decorative: these are the topics the double GRANTS, and
                    // they are translated into broker ACL exceptions below. The application can publish to
                    // exactly this list and nothing else, which is what lets a scenario prove authorisation
                    // rather than just authentication. Both directions are granted on the AsyncAPI's channel.
                    permissions: permissionsFromChannels(),
                    messagingServiceConnections: [],
                };
                reg.accessRequests.push(ar);
                // Provision the grant on the REAL broker before answering, for the same reason the credentials
                // route awaits its SEMP call: a scenario that publishes as soon as the subscription returns
                // would otherwise race the ACL write and be denied.
                ensureAclProfile(seg[1], ar.permissions)
                        .then(() => send(res, 201, { data: ar, meta: {} }))
                        .catch((error) => {
                            log(`     ERROR provisioning access request for registration ${seg[1]}: ${error.stack || error}`);
                            send(res, 500, { message: `failed to provision access request for registration ${seg[1]}` });
                        });
                return;
            }
            if (m === 'GET') return send(res, 200, { data: reg.accessRequests, meta: {} });
        }

        // ---- DELETE /appRegistrations/{id}/accessRequests/{accessRequestId} ------------------
        if (m === 'DELETE' && seg.length === 4 && seg[0] === 'appRegistrations' && seg[2] === 'accessRequests') {
            const reg = state.registrations[seg[1]];
            if (reg) reg.accessRequests = reg.accessRequests.filter((a) => a.accessRequestId !== seg[3]);
            return send(res, 200, { accessRequestId: seg[3], deleted: true });
        }

        // ---- debugging only; never assert against this ----------------------------------------
        if (m === 'GET' && seg[0] === '_mock' && seg[1] === 'state') return send(res, 200, state);
        if (m === 'POST' && seg[0] === '_mock' && seg[1] === 'reset') {
            state.registrations = {}; seq = 0; return send(res, 200, { reset: true });
        }

        // ---- fallthrough: THE POINT OF THIS SERVER --------------------------------------------
        // Anything APIM calls that is not implemented above shows up here, loudly, with its body.
        // That is how you discover the real contract instead of inferring it.
        log(`  -> 404 UNMATCHED  ${m} ${url.pathname}  <-- add a route for this`);
        return send(res, 404, { message: 'unmatched route in mock', method: m, path: url.pathname });
    });
});

server.listen(PORT, () => {
    /*
     * Configure the broker's client authentication AT STARTUP, not on first use.
     *
     * Why it matters: a "bad token is rejected" scenario needs no APIM setup at all -- it just publishes with
     * a bogus token. But an untouched broker accepts ANY credential (see authenticationBasicEnabled above).
     * If this ran lazily on the first credential push, that scenario's verdict would depend on whether some
     * OTHER scenario had already run -- ordering-dependent, and forbidden by the isolation rule. Doing it
     * here makes every scenario see the same broker regardless of order.
     *
     * This does NOT require APIM to be up: nothing is fetched now, only configured. The broker fetches the
     * JWKS lazily when it first validates a token, which is long after APIM has booted.
     * The broker starts BEFORE this container (DynamicSolaceBroker.start), so SEMP is already answering.
     */
    configureApimOAuth().catch((error) => {
        log(`     ERROR configuring broker OAuth: ${error.stack || error}`);
    });
    log(`listening on http://localhost:${PORT}`);
    log(`set  [apim.solace_config]  apim_api_endpoint = "http://localhost:${PORT}"  token = "anything"`);
    log(`ids: product=${PRODUCT_ID} plan=${PLAN_ID} eventApi=${EVENT_API_ID}`);
});
