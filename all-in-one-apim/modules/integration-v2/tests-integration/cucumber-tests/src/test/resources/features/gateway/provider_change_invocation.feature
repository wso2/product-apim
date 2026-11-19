@cleanup
Feature: Gateway Invocation After An API Provider Change

  Gateway-plane runtime half of the API provider (ownership) transfer: after an API is re-owned by a different
  provider, the deployed revision is undeployed, a NEW revision is created and deployed by the new owner, and the
  API is still invocable through the gateway with the SAME application token (200). Covers REST, SOAP passthrough,
  SOAP-to-REST and GraphQL types. This block opts into the node backend (initBackend), which a gateway invocation requires (§11); the
  publisher-plane retention assertions of the same transfer live in publisher/api_provider_change. Ports the
  invocation legs of ChangeApiProviderTestCase. Teardown via the per-scenario cleanup hook.

  # Runs with primary and SECONDARY.COM new providers in BOTH tenants, proving tenant and user-store resolution
  # across the same post-transfer revision/deploy/invocation arc.
  @cap:gateway @feat:rest-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A REST API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "pciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # Mint the new provider's publisher tokens so the post-transfer revision legs can run AS them.
    And The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I act as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pciApiId" and deployed it
    # Keep the deployed revision's id: it is undeployed and replaced AFTER the provider change.
    And I put value "revisionId" in context as "pciRevision1"
    And the "apis" resource "pciApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "pciRevision1"
    When I publish the "apis" resource with id "pciApiId"
    Then The lifecycle status of API "pciApiId" should be "Published"
    When I retrieve the "apis" resource with id "pciApiId"
    And I extract response field "context" and store it as "pciApiContext"

    # BASELINE: the API is invocable through the gateway BEFORE the ownership transfer.
    When I have set up application with keys, subscribed to API "pciApiId", and obtained access token for "pciSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{pciApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

    # Transfer ownership.
    When I change the provider of API "pciApiId" to "<providerActor>"
    Then The response status code should be 200
    And The provider of API "pciApiId" should match actor "<providerActor>"
    # The undeploy/revision/deploy legs below run AS THE NEW PROVIDER — the feature header's claim.
    When I act as "<providerActor>"

    # Undeploy the pre-change revision and deploy a NEW revision created after the change.
    When I undeploy revision "pciRevision1" of "apis" resource "pciApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pciRevisionPayload"
    """
    {"description":"Revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "pciApiId" with payload "pciRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "pciRevision2"
    When I deploy revision "pciRevision2" of "apis" resource "pciApiId"
    Then The response status code should be 201
    And the "apis" resource "pciApiId" should be live on the gateway, redeploying if propagation is lost

    # The re-owned API is invocable again through the gateway with the SAME application token.
    When I invoke the API at gateway context "{{pciApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"

  Examples:
    | actor | tenant | suffix | providerActor |
    | admin | carbon.super |  | pciNewProvider |
    | admin | carbon.super |  | SECONDARY.COM/publisherUser1 |
    | admin@tenant1.com | tenant1.com | @tenant1.com | pciNewProvider@tenant1.com |
    | admin@tenant1.com | tenant1.com | @tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # SOAP counterpart of the REST/GraphQL provider-change invocation arcs. The SOAP API is invocable before the
  # transfer, then its old revision is replaced by a revision deployed as the new provider and the same SOAP
  # application token invokes it again. Publisher-side WSDL retention and revision snapshot checks are covered in
  # publisher/api_provider_change; this scenario covers the gateway runtime behavior after ownership changes.
  @cap:gateway @feat:soap-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A SOAP API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "pciSoapNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I act as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_soap_api.json" as "pciSoapApiId" and deployed it
    And I put value "revisionId" in context as "pciSoapRevision1"
    And the "apis" resource "pciSoapApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "pciSoapRevision1"
    When I publish the "apis" resource with id "pciSoapApiId"
    Then The lifecycle status of API "pciSoapApiId" should be "Published"
    When I retrieve the "apis" resource with id "pciSoapApiId"
    And I extract response field "context" and store it as "pciSoapApiContext"

    # Establish the SOAP route before transfer, using the application token that will also exercise the new revision.
    When I have set up application with keys, subscribed to API "pciSoapApiId", and obtained access token for "pciSoapSubId"
    Then The response status code should be 200
    # Warm the newly published gateway route before asserting the SOAP operation itself.
    When I invoke the API at gateway context "{{pciSoapApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I put the following JSON payload in context as "pciSoapRequest"
    """
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns="http://ws.cdyne.com/PhoneVerify/query">
      <soapenv:Body>
        <ns:CheckPhoneNumber>
          <ns:PhoneNumber>18006785432</ns:PhoneNumber>
          <ns:LicenseKey>0</ns:LicenseKey>
        </ns:CheckPhoneNumber>
      </soapenv:Body>
    </soapenv:Envelope>
    """
    And I invoke the SOAP API at gateway context "{{pciSoapApiContext}}/1.0.0" using access token "generatedAccessToken" and payload "pciSoapRequest" and soap action "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber"
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"
    And The response should contain "SOAP Stub"

    When I change the provider of API "pciSoapApiId" to "<providerActor>"
    Then The response status code should be 200
    And The provider of API "pciSoapApiId" should match actor "<providerActor>"
    When I act as "<providerActor>"
    When I undeploy revision "pciSoapRevision1" of "apis" resource "pciSoapApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pciSoapRevisionPayload"
    """
    {"description":"SOAP revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "pciSoapApiId" with payload "pciSoapRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "pciSoapRevision2"
    When I deploy revision "pciSoapRevision2" of "apis" resource "pciSoapApiId"
    Then The response status code should be 201
    And the "apis" resource "pciSoapApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "pciSoapRevision2"

    # The same subscriber token must continue to route the SOAP request after transfer and redeployment.
    When I invoke the SOAP API at gateway context "{{pciSoapApiContext}}/1.0.0" using access token "generatedAccessToken" and payload "pciSoapRequest" and soap action "http://ws.cdyne.com/PhoneVerify/query/CheckPhoneNumber"
    Then The response status code should be 200
    And The response should contain "CheckPhoneNumberResponse"
    And The response should contain "SOAP Stub"

  Examples:
    | actor | tenant | suffix | providerActor |
    | admin | carbon.super |  | pciSoapNewProvider |
    | admin | carbon.super |  | SECONDARY.COM/publisherUser1 |
    | admin@tenant1.com | tenant1.com | @tenant1.com | pciSoapNewProvider@tenant1.com |
    | admin@tenant1.com | tenant1.com | @tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # SOAP-to-REST counterpart: the generated sayHello resource and its REST-to-SOAP conversion must still work
  # after ownership transfer and deployment of a revision by the new provider. The ordinary SOAP-to-REST
  # invocation is covered in gateway/soap_invocation; this pins the provider-change interaction on that API type.
  @cap:gateway @feat:soap-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A SOAP-to-REST API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "pciS2rNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    And The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I act as "<actor>"
    And I generate a unique value and store it as "pciS2rApiName"
    And I generate a unique value and store it as "pciS2rApiContext"
    When I put the following JSON payload in context as "pciS2rAdditionalProperties"
    """
    {"name":"{{pciS2rApiName}}","context":"/{{pciS2rApiContext}}","version":"1.0.0","policies":["Unlimited","Bronze"],"endpointConfig":{"endpoint_type":"http","production_endpoints":{"url":"http://nodebackend:3019/service"},"sandbox_endpoints":{"url":"http://nodebackend:3019/service"}}}
    """
    And I import a WSDL API from file "artifacts/wsdl/hello.wsdl" with additional properties "pciS2rAdditionalProperties" and implementation type "SOAPTOREST" as "pciS2rApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pciS2rInitialRevisionPayload"
    """
    {"description":"Initial revision before provider change"}
    """
    And I make a request to create a revision for "apis" resource "pciS2rApiId" with payload "pciS2rInitialRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "pciS2rRevision1"
    When I deploy revision "pciS2rRevision1" of "apis" resource "pciS2rApiId"
    Then The response status code should be 201
    And the "apis" resource "pciS2rApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "pciS2rRevision1"
    When I publish the "apis" resource with id "pciS2rApiId"
    Then The lifecycle status of API "pciS2rApiId" should be "Published"
    When I retrieve the "apis" resource with id "pciS2rApiId"
    And I extract response field "context" and store it as "pciS2rContext"
    When I have set up application with keys, subscribed to API "pciS2rApiId", and obtained access token for "pciS2rSubId"
    Then The response status code should be 200

    # Baseline invocation proves the generated conversion works before the provider changes.
    When I put the following JSON payload in context as "pciS2rBody"
    """
    {"name":"WSO2"}
    """
    And I invoke the API at gateway context "{{pciS2rContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "pciS2rBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"

    When I change the provider of API "pciS2rApiId" to "<providerActor>"
    Then The response status code should be 200
    And The provider of API "pciS2rApiId" should match actor "<providerActor>"
    When I act as "<providerActor>"
    When I undeploy revision "pciS2rRevision1" of "apis" resource "pciS2rApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pciS2rRevisionPayload"
    """
    {"description":"Revision created after SOAP-to-REST provider change"}
    """
    And I make a request to create a revision for "apis" resource "pciS2rApiId" with payload "pciS2rRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "pciS2rRevision2"
    When I deploy revision "pciS2rRevision2" of "apis" resource "pciS2rApiId"
    Then The response status code should be 201
    And the "apis" resource "pciS2rApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "pciS2rRevision2"

    # The same application token must still traverse the REST-to-SOAP and SOAP-to-REST sequences after transfer.
    When I invoke the API at gateway context "{{pciS2rContext}}/1.0.0/sayHello" with method "POST" using access token "generatedAccessToken" and payload "pciS2rBody" with content type "application/json" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "SOAP Stub"

    Examples:
      | actor | tenant | suffix | providerActor |
      | admin | carbon.super |  | pciS2rNewProvider |
      | admin | carbon.super |  | SECONDARY.COM/publisherUser1 |
      | admin@tenant1.com | tenant1.com | @tenant1.com | pciS2rNewProvider@tenant1.com |
      | admin@tenant1.com | tenant1.com | @tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |

  # GraphQL counterpart. Uses the SELF-HEALING deploy gate ("should be live on the gateway, redeploying if
  # propagation is lost", §15) rather than a plain retry, as the other GraphQL gateway features do — a dropped
  # at-most-once deploy event can only be fixed by re-emitting it. Runs x2 tenants like the REST counterpart.
  @cap:gateway @feat:graphql-invocation @rule:provider-change @type:regression @dep:publisher @dep:admin @legacy:ChangeApiProviderTestCase @legacy:ChangeApiProviderSecondaryUserStoreTestCase
  Scenario Outline: A GraphQL API stays invocable after its provider changes and a new revision is deployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "gciNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"
    # Mint the new provider's publisher tokens so the post-transfer revision legs can run AS them.
    And The system is ready and I have valid publisher access tokens as "<providerActor>"
    And I act as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_graphql_api.json" in context as "gciApiPayload"
    And I create a GraphQL API with schema file "artifacts/payloads/graphql_schema.graphql" and additional properties "gciApiPayload" as "gciApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "gciApiId"
    And I put the response payload in context as "gciRetrievedPayload"
    And I extract response field "context" and store it as "gciApiContext"

    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {"description":"Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "gciApiId" with payload "createRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "gciRevision1"
    When I deploy revision "gciRevision1" of "apis" resource "gciApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "gciRetrievedPayload"
    And the "apis" resource "gciApiId" should be live on the gateway, redeploying if propagation is lost and updating revision key "gciRevision1"
    And I publish the "apis" resource with id "gciApiId"
    Then The lifecycle status of API "gciApiId" should be "Published"

    # BASELINE: the GraphQL query resolves through the gateway BEFORE the ownership transfer.
    When I have set up application with keys, subscribed to API "gciApiId", and obtained access token for "gciSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "gciQuery"
    """
    {"query": "{languages{code name}}"}
    """
    And I invoke the API at gateway context "{{gciApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gciQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Afrikaans"

    # Transfer ownership.
    When I change the provider of API "gciApiId" to "<providerActor>"
    Then The response status code should be 200
    And The provider of API "gciApiId" should match actor "<providerActor>"
    # The undeploy/revision/deploy legs below run AS THE NEW PROVIDER — the feature header's claim.
    When I act as "<providerActor>"

    # Undeploy the pre-change revision and deploy a NEW revision created after the change.
    When I undeploy revision "gciRevision1" of "apis" resource "gciApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "gciRevisionPayload"
    """
    {"description":"Revision created after the provider change"}
    """
    And I make a request to create a revision for "apis" resource "gciApiId" with payload "gciRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "gciRevision2"
    When I deploy revision "gciRevision2" of "apis" resource "gciApiId"
    Then The response status code should be 201
    And the "apis" resource "gciApiId" should be live on the gateway, redeploying if propagation is lost

    # The re-owned GraphQL API resolves the same query again with the SAME application token.
    When I invoke the API at gateway context "{{gciApiContext}}/1.0.0" with method "POST" using access token "generatedAccessToken" and payload "gciQuery" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Afrikaans"

    Examples:
      | actor | tenant | suffix | providerActor |
      | admin | carbon.super |  | gciNewProvider |
      | admin | carbon.super |  | SECONDARY.COM/publisherUser1 |
      | admin@tenant1.com | tenant1.com | @tenant1.com | gciNewProvider@tenant1.com |
      | admin@tenant1.com | tenant1.com | @tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com |
