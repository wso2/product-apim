@cleanup
Feature: Solace event-API integration

  APIM discovers and imports Event Portal definitions through the mocked Solace control plane. Subscription
  and credential notifiers drive that mock; authorization is observed through a real Solace broker.
  The mock derives topic ACLs from its own AsyncAPI fixture because APIM sends product/plan identifiers,
  not channel permissions. Broker grant/denial checks validate this combined fixture, not APIM topic translation.
  The broker receives both tenants' public signing keys for their shared issuer and verifies their JWT signatures.

  Metadata and lifecycle rows assert APIM responses. They do not establish remote Event API Product removal.
  Unsubscribe revocation is a separate obligation from gateway undeployment. The current gateway dispatcher needs
  a GatewayDeployer; the shipped Solace deployer implements the older ExternalGatewayDeployer interface. Remote
  removal on undeploy, retirement and API deletion remains blocked without a working deployment path.
  The deployment negative advertises the Solace type and an external provider to exercise the missing-deployer
  response. It does not establish a successful Solace gateway deployment.

  Automatic key generation remains super-tenant-only: the tenant workflow invokes the key notifier with a null
  username after committing keys. Broker revocation uses actor-owned mapped clients before subscription in both
  tenants, preserving the same-token allow/deny controls. This workaround does not close automatic-keygen parity.

  Background:
    Given The system is ready

  @cap:publisher @feat:solace @rule:solace-discovery @type:smoke @legacy:SolaceTestCase
  Scenario Outline: Event API Products are discoverable through APIM as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the integrated Solace APIs
    Then The response status code should be 200
    # Discovery composes product/plan/API IDs and the event API name/version.
    And The response should contain "8xk2p9qz1a/pl4n7bq2xd/api5t8w2rk"
    And The response should contain "SolaceEventApi:1.0.0"
    And The response should contain "AsyncUnlimited"

    Examples:
      | actor          |
      | publisherUser  |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-discovery @type:regression @legacy:SolaceTestCase
  Scenario Outline: The AsyncAPI definition of an Event API Product is retrievable through APIM as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the integrated Solace API definition for product "8xk2p9qz1a" plan "pl4n7bq2xd" api "api5t8w2rk"
    Then The response status code should be 200
    And The response should contain "x-ep-event-api-product-id"
    And The response should contain "x-ep-application-domain-id"

    Examples:
      | actor          |
      | publisherUser  |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-import @type:regression @legacy:SolaceTestCase
  Scenario Outline: An AsyncAPI definition is imported as a Solace event API as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I validate the asyncapi definition from file "artifacts/payloads/solace/solace_asyncapi.yaml"
    Then The response status code should be 200
    And The value of response field "isValid" should be "true"
    When I put JSON payload from file "artifacts/payloads/solace/solace_api_props.json" in context as "solaceImportProperties"
    And I extract field "name" from json payload "solaceImportProperties" and store it as "solaceExpectedName"
    And I extract field "context" from json payload "solaceImportProperties" and store it as "solaceExpectedContext"
    And I extract field "version" from json payload "solaceImportProperties" and store it as "solaceExpectedVersion"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "solaceImportProperties" as "solaceApiId"
    Then The response status code should be 201
    And The value of response field "name" should be "{{solaceExpectedName}}"
    And The value of response field "context" should be "<tenantPrefix>{{solaceExpectedContext}}"
    # gatewayVendor is normalized; gatewayType carries the Solace identity on Publisher responses.
    And The value of response field "gatewayVendor" should be "external"
    And The value of response field "gatewayType" should be "solace"
    # Publisher derives protocols from operation bindings; servers supply DevPortal endpoint metadata.
    And The response field "asyncTransportProtocols" should be exactly the list "mqtt,http"
    And The response array field "asyncTransportProtocols" should have exactly 2 entries
    When I retrieve the "apis" resource with id "solaceApiId"
    Then The response status code should be 200
    # gatewayVendor is normalized; gatewayType carries the Solace identity on Publisher responses.
    And The value of response field "gatewayVendor" should be "external"
    And The value of response field "gatewayType" should be "solace"
    And The value of response field "name" should be "{{solaceExpectedName}}"
    And The value of response field "context" should be "<tenantPrefix>{{solaceExpectedContext}}"
    And The value of response field "type" should be "WEBSUB"
    And The response field "asyncTransportProtocols" should be exactly the list "mqtt,http"
    And The response array field "asyncTransportProtocols" should have exactly 2 entries

    Examples:
      | actor                     | tenantPrefix   |
      | publisherUser             |                |
      | publisherUser@tenant1.com | /t/tenant1.com |

  @cap:devportal @feat:solace @rule:solace-visibility @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: A published Solace event API is visible to consumers in the developer portal as <actor>
    Given I have valid access tokens as "<actor>"
    When I validate the asyncapi definition from file "artifacts/payloads/solace/solace_asyncapi.yaml"
    Then The response status code should be 200
    And The value of response field "isValid" should be "true"
    When I put JSON payload from file "artifacts/payloads/solace/solace_api_props.json" in context as "solaceStoreProperties"
    And I extract field "name" from json payload "solaceStoreProperties" and store it as "solaceExpectedName"
    And I extract field "context" from json payload "solaceStoreProperties" and store it as "solaceExpectedContext"
    And I extract field "version" from json payload "solaceStoreProperties" and store it as "solaceExpectedVersion"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "solaceStoreProperties" as "solaceStoreApiId"
    Then The response status code should be 201
    And The value of response field "name" should be "{{solaceExpectedName}}"
    And The value of response field "context" should be "<tenantPrefix>{{solaceExpectedContext}}"
    # gatewayVendor is normalized; gatewayType carries the Solace identity on Publisher responses.
    And The value of response field "gatewayVendor" should be "external"
    And The value of response field "gatewayType" should be "solace"
    # Publisher derives protocols from operation bindings; servers supply DevPortal endpoint metadata.
    And The response field "asyncTransportProtocols" should be exactly the list "mqtt,http"
    And The response array field "asyncTransportProtocols" should have exactly 2 entries
    When I change the lifecycle of API "solaceStoreApiId" with action "Publish"
    Then The response status code should be 200
    When I retrieve the devportal API "solaceStoreApiId" until the response status code becomes 200 within 60 seconds
    # gatewayType distinguishes Solace from other external gateway integrations.
    And The value of response field "gatewayVendor" should be "external"
    And The value of response field "gatewayType" should be "solace"
    And The value of response field "name" should be "{{solaceExpectedName}}"
    And The value of response field "context" should be "<tenantPrefix>{{solaceExpectedContext}}/{{solaceExpectedVersion}}"
    # DevPortal represents each protocol/endpoint entry as a JSON-encoded string.
    And The Solace DevPortal transport protocols should be exactly:
      | protocol | endPointUrl       |
      | smf      | solacebroker:55555 |
      | mqtt     | solacebroker:1883  |
    When I retrieve the topics of devportal API "solaceStoreApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"

    Examples:
      | actor             | tenantPrefix   |
      | admin             |                |
      | admin@tenant1.com | /t/tenant1.com |

  @cap:devportal @feat:solace @rule:solace-subscription @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: A Solace subscription is counted and its application can be deleted after unsubscribe as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceAppPayload"
    And I create an application with payload "solaceAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "solaceApiId" using application "createdAppId" with payload "solaceSubPayload" as "solaceSubId"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The value of response field "subscriptionCount" should be "1"
    When I retrieve the Solace application registration for application "createdAppId"
    Then The response status code should be 200
    And The value of response field "data.registrationId" should be "{{createdAppId}}"
    And The response array field "data.accessRequests" should have exactly 1 entries

    # Delete-after-unsubscribe is the supported compatibility ordering exercised here. Direct application
    # deletion while subscribed returns 500 after APIM removes the application/subscription rows. The
    # subscription notifier reloads that removed application and throws before remote registration cleanup.
    When I delete the subscription with id "solaceSubId"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The value of response field "subscriptionCount" should be "0"
    # Unsubscribe removes the access request while retaining the known application registration.
    When I retrieve the Solace application registration for application "createdAppId"
    Then The response status code should be 200
    And The value of response field "data.registrationId" should be "{{createdAppId}}"
    And The response array field "data.accessRequests" should have exactly 0 entries
    When I delete the application with id "createdAppId"
    Then The response status code should be 200
    When I retrieve the Solace application registration for application "createdAppId"
    Then The response status code should be 404
    And The value of error response field "message" should be "registration {{createdAppId}} not found"

    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-retire @type:regression @legacy:SolaceTestCase
  Scenario Outline: A published Solace event API can be deprecated and retired as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceRetireApiId"
    Then The response status code should be 201
    And The value of response field "gatewayType" should be "solace"
    When I change the lifecycle of API "solaceRetireApiId" with action "Publish"
    Then The response status code should be 200
    # These are APIM lifecycle outcomes; no Solace-side removal is asserted by this row.
    When I change the lifecycle of API "solaceRetireApiId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Deprecated"
    When I retrieve the "apis" resource with id "solaceRetireApiId"
    Then The response status code should be 200
    And The value of response field "lifeCycleStatus" should be "DEPRECATED"
    When I change the lifecycle of API "solaceRetireApiId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "lifecycleState.state" should be "Retired"
    When I retrieve the "apis" resource with id "solaceRetireApiId"
    Then The response status code should be 200
    And The value of response field "lifeCycleStatus" should be "RETIRED"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:solace @rule:solace-delete @type:regression @legacy:SolaceTestCase
  Scenario Outline: A published Solace event API can be deleted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceDeleteApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceDeleteApiId" with action "Publish"
    Then The response status code should be 200
    # This pins APIM deletion only. Remote product removal is a separate, unresolved contract.
    When I delete the "apis" resource with id "solaceDeleteApiId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:devportal @feat:solace @rule:solace-keygen @type:regression @dep:publisher @legacy:SolaceTestCase
  Scenario Outline: Generating application keys pushes the credentials to Solace as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceKeyApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceKeyApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceKeyAppPayload"
    And I create an application with payload "solaceKeyAppPayload"
    Then The response status code should be 201
    # Subscribe before key generation: the Solace notifier needs an existing Solace subscription.
    When I put the following JSON payload in context as "solaceKeySubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "solaceKeyApiId" using application "createdAppId" with payload "solaceKeySubPayload" as "solaceKeySubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceKeygenPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "solaceKeygenPayload"
    Then The response status code should be 200
    # Key-generation HTTP 200 alone cannot prove propagation; SEMP independently observes provisioning.
    When I retrieve the Solace broker client username for consumer key "{{consumerKey}}"
    Then The response status code should be 200
    Examples:
      | actor |
      | admin |

  @cap:gateway @feat:solace @rule:solace-revocation @type:regression @dep:publisher @dep:devportal @legacy:SolaceTestCase
  Scenario Outline: Unsubscribing revokes Solace broker access without affecting another application as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceInvokeApiId"
    Then The response status code should be 201
    When I change the lifecycle of API "solaceInvokeApiId" with action "Publish"
    Then The response status code should be 200
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceInvokeAppPayload"
    And I create an application with payload "solaceInvokeAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceInvokeSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    # The tenant generate-keys workflow invokes the Solace key notifier with a null username (NPE/500).
    # Register/map an actor-owned client before subscribing; the subscription notifier propagates its keys.
    # This verifies mapped credentials in both tenants; successful automatic tenant key generation stays blocked.
    When I register an OAuth client "solaceMapped" as "solaceMapped" with token type "JWT"
    Then The response status code should be 200
    When I map OAuth client "solaceMapped" to application "createdAppId" via key manager "Resident Key Manager"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "consumerKey"
    And I extract response field "consumerSecret" and store it as "consumerSecret"
    And I extract response field "keyMappingId" and store it as "keyMappingId"
    And I subscribe to API "solaceInvokeApiId" using application "createdAppId" with payload "solaceInvokeSubPayload" as "solaceInvokeSubId"
    Then The response status code should be 201
    And I copy context value "createdAppId" to "revokedSolaceApplicationId"
    And I copy context value "keyMappingId" to "revokedSolaceKeyMappingId"
    When I retrieve the Solace broker client username for consumer key "{{consumerKey}}"
    Then The response status code should be 200
    And The value of response field "data.clientUsername" should be "{{consumerKey}}"
    And I extract response field "data.aclProfileName" and store it as "revokedSolaceAclProfileName"
    When I retrieve the Solace broker ACL profile "{{revokedSolaceAclProfileName}}"
    Then The response status code should be 200
    And The value of response field "data.aclProfileName" should be "{{revokedSolaceAclProfileName}}"
    When I retrieve the Solace application registration for application "revokedSolaceApplicationId"
    Then The response status code should be 200
    And The value of response field "data.registrationId" should be "{{revokedSolaceApplicationId}}"
    When I put the following JSON payload in context as "solaceInvokeTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "solaceInvokeTokenPayload"
    Then The response status code should be 200
    And The response field "accessToken" should be non-blank
    When I publish an event to the Solace topic "orders/created" with OAuth token "{{generatedAccessToken}}" until the response status code becomes 200 within 60 seconds
    # The mock seeds topic permissions from its own definition; the real broker enforces that ACL.
    # This uses the same credential as the allowed-topic control and does not prove APIM channel translation.
    When I publish an event to the Solace topic "orders/forbidden" with OAuth token "{{generatedAccessToken}}" until the response status code becomes 403 within 30 seconds
    Then The XML error response at XPath "/solace-error-response/code" should be "403"
    And The XML error response at XPath "/solace-error-response/reason" should be "Publish ACL Denied"
    When I subscribe over MQTT to the Solace topic "orders/created" with OAuth token "{{generatedAccessToken}}" expecting "granted"
    When I subscribe over MQTT to the Solace topic "orders/forbidden" with OAuth token "{{generatedAccessToken}}" expecting "denied"
    # Retain the same credential; an independently subscribed application controls for broker-wide failure.
    And I copy context value "generatedAccessToken" to "revokedSolaceToken"
    And I copy context value "consumerKey" to "revokedSolaceConsumerKey"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "solaceControlAppPayload"
    And I create an application with payload "solaceControlAppPayload"
    Then The response status code should be 201
    When I register an OAuth client "solaceMappedControl" as "solaceMappedControl" with token type "JWT"
    Then The response status code should be 200
    When I map OAuth client "solaceMappedControl" to application "createdAppId" via key manager "Resident Key Manager"
    Then The response status code should be 200
    And I extract response field "consumerKey" and store it as "consumerKey"
    And I extract response field "consumerSecret" and store it as "consumerSecret"
    And I extract response field "keyMappingId" and store it as "keyMappingId"
    And I subscribe to API "solaceInvokeApiId" using application "createdAppId" with payload "solaceInvokeSubPayload" as "solaceControlSubId"
    Then The response status code should be 201
    And I request an access token for application id "createdAppId" using payload "solaceInvokeTokenPayload"
    Then The response status code should be 200
    And I copy context value "generatedAccessToken" to "controlSolaceToken"
    When I publish an event to the Solace topic "orders/created" with OAuth token "{{controlSolaceToken}}" until the response status code becomes 200 within 60 seconds
    When I subscribe over MQTT to the Solace topic "orders/created" with OAuth token "{{controlSolaceToken}}" expecting "granted"

    # APIM must delete a known access request. The mock reconciles remaining grants before replying;
    # the unchanged JWT must still authenticate while the real broker denies its revoked topic.
    When I delete the subscription with id "solaceInvokeSubId"
    Then The response status code should be 200
    When I retrieve the Solace broker client username for consumer key "{{revokedSolaceConsumerKey}}"
    Then The response status code should be 200
    When I publish an event to the Solace topic "orders/created" with OAuth token "{{revokedSolaceToken}}" until the response status code becomes 403 within 30 seconds
    Then The XML error response at XPath "/solace-error-response/code" should be "403"
    And The XML error response at XPath "/solace-error-response/reason" should be "Publish ACL Denied"
    # Fresh MQTT connection: CONNACK 0 establishes authentication before SUBACK 0x80 establishes denial.
    When I subscribe over MQTT to the Solace topic "orders/created" with OAuth token "{{revokedSolaceToken}}" expecting "denied"
    # APIM's deletion event re-enters the key notifier after its application row is gone (NPE/500).
    # Delete the OAuth keys first to avoid that event; the remote registration/client/profile must still exist.
    When I delete the keys for application "revokedSolaceApplicationId" with key mapping "revokedSolaceKeyMappingId"
    Then The response status code should be 200
    When I retrieve the Solace application registration for application "revokedSolaceApplicationId"
    Then The response status code should be 200
    When I retrieve the Solace broker client username for consumer key "{{revokedSolaceConsumerKey}}"
    Then The response status code should be 200
    When I retrieve the Solace broker ACL profile "{{revokedSolaceAclProfileName}}"
    Then The response status code should be 200
    # Deleting the unsubscribed application removes its registration and its exact real-broker resources.
    # The independently subscribed control application must remain authorized afterward.
    When I delete the application with id "revokedSolaceApplicationId"
    Then The response status code should be 200
    When I retrieve the Solace application registration for application "revokedSolaceApplicationId"
    Then The response status code should be 404
    And The value of error response field "message" should be "registration {{revokedSolaceApplicationId}} not found"
    When I retrieve the Solace broker client username for consumer key "{{revokedSolaceConsumerKey}}"
    Then The response status code should be 400
    And The value of error response field "meta.error.code" should be "6"
    And The value of error response field "meta.error.status" should be "NOT_FOUND"
    When I retrieve the Solace broker ACL profile "{{revokedSolaceAclProfileName}}"
    Then The response status code should be 400
    And The value of error response field "meta.error.code" should be "6"
    And The value of error response field "meta.error.status" should be "NOT_FOUND"
    When I publish an event to the Solace topic "orders/created" with OAuth token "{{controlSolaceToken}}" until the response status code becomes 200 within 60 seconds
    When I subscribe over MQTT to the Solace topic "orders/created" with OAuth token "{{controlSolaceToken}}" expecting "granted"
    # Keep unsubscribe-before-application-delete ordering in the compatibility flow.
    When I delete the subscription with id "solaceControlSubId"
    Then The response status code should be 200
    Examples:
      | actor |
      | admin |
      | admin@tenant1.com |

  @cap:gateway @feat:solace @rule:solace-invoke @type:negative
  Scenario: The Solace broker rejects a publish with an invalid token
    # This row verifies broker configuration: OAuth is required and the fallback username is disabled.
    When I publish an event to the Solace topic "orders/created" with OAuth token "not-a-valid-token" until the response status code becomes 403 within 30 seconds
    Then The XML error response at XPath "/solace-error-response/code" should be "403"
    And The XML error response at XPath "/solace-error-response/reason" should be "Unauthorized"

  @cap:publisher @feat:solace @rule:solace-import @type:negative
  Scenario Outline: A Solace gateway deployment reports its missing deployer as <actor>
    Given I have valid access tokens as "<actor>"
    When I put the following JSON payload in context as "solaceGatewayEnvironmentPayload"
    """
    {"name":"${UNIQUE:SolaceReviewGateway}","displayName":"Solace deployment diagnostic","description":"Missing Solace gateway deployer diagnostic","provider":"solace","gatewayType":"solace","isReadOnly":false,"vhosts":[{"host":"solace.review.invalid","httpContext":"","httpPort":8280,"httpsPort":8243,"wsPort":9099,"wssPort":8099}]}
    """
    And I create a gateway environment with payload "solaceGatewayEnvironmentPayload"
    Then The response status code should be 201
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    And The value of response field "gatewayType" should be "solace"
    And The value of response field "provider" should be "solace"
    And I extract response field "name" and store it as "solaceDeployGatewayName"
    When I import asyncapi definition from "artifacts/payloads/solace/solace_asyncapi.yaml" with additional properties "artifacts/payloads/solace/solace_api_props.json" as "solaceDeployApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceDeployRevisionPayload"
    """
    {"description":"Solace gateway deployer availability diagnostic"}
    """
    And I make a request to create a revision for "apis" resource "solaceDeployApiId" with payload "solaceDeployRevisionPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "solaceDeployPayload"
    """
    [{"name":"{{solaceDeployGatewayName}}","vhost":"solace.review.invalid","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "solaceDeployApiId" with payload "solaceDeployPayload"
    Then The response status code should be 207
    # APIM carries its deployment error DTO in HTTP Multi-Status, which is in the 2xx range.
    And The value of response field "code" should be "902060"
    And The value of response field "message" should be "Error while deploying API to Gateway"
    And The value of response field "description" should be "Error while deploying API to Gateway. {{solaceDeployApiId}}"
    When I retrieve the deployed revisions of "apis" resource "solaceDeployApiId"
    Then The response status code should be 200
    And The response array field "list" should have exactly 0 entries

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
