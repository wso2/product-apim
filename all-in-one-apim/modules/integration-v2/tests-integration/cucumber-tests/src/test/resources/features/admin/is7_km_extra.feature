@cap:admin @feat:external-key-manager @cleanup
Feature: External Key Manager Coexistence, Permissions and Grant Handling

  Additional IS7 external-key-manager behaviors, each self-contained (own resources, per-scenario cleanup): a
  resident-KM application and an IS7-KM application both invoke the same API independently (multiple KMs coexist);
  a user in a role the KM denies cannot generate keys against it (403); and requesting a grant the KM does not
  list in its available grant types is (per the pinned behavior) accepted at key generation rather than filtered.
  Each scenario registers its own key manager inline: this feature is per-scenario @cleanup, whose sweep
  would delete a runner-shared fixture KM out from under the scenarios that follow (validated: 901403).
  Note on the DENY scenario: the generate-keys endpoint checks isKeyManagerAllowedForUser(keyManager, user) first,
  and that permission lookup is BY KEY-MANAGER UUID - so the keygen payload must reference the KM by its id
  ({{denyKm}}), not its name, or the permission row is not found and the DENY is silently skipped (the keygen then
  proceeds and fails later with an unrelated 409).

  @rule:coexistence @type:regression @dep:gateway
  Scenario: An IS7-KM application and a resident-KM application both invoke the same API independently
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "coexKm"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "multiApiId" and deployed it
    When I publish the "apis" resource with id "multiApiId"
    Then The lifecycle status of API "multiApiId" should be "Published"
    When I retrieve the "apis" resource with id "multiApiId"
    And I extract response field "context" and store it as "multiApiContext"
    # --- IS7-KM application (token issued by IS) ---
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "is7AppPayload"
    And I create an application with payload "is7AppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "is7KeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{coexKmName}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "is7KeygenPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "is7SubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "multiApiId" using application "createdAppId" with payload "is7SubPayload" as "is7SubId"
    Then The response status code should be 201
    When I request an OAuth access token from the external key manager using client credentials grant
    Then The response status code should be 200
    And I invoke the API at gateway context "{{multiApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # --- Resident-KM application (token issued by APIM) ---
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "resAppPayload"
    And I create an application with payload "resAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "resKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "resKeygenPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "resSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "multiApiId" using application "createdAppId" with payload "resSubPayload" as "resSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "resTokenPayload"
    """
    {"consumerSecret": "{{appConsumerSecret}}", "validityPeriod": 3600}
    """
    And I request an access token for application id "createdAppId" using payload "resTokenPayload"
    And I invoke the API at gateway context "{{multiApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

  @rule:km-permissions @type:negative
  Scenario: A user in a role the WSO2-IS-7 key manager denies cannot generate keys against it
    # The KM denies Internal/subscriber; subscriberUser (who holds that role) is refused key generation with 403.
    # The keygen references the KM by its UUID ({{denyKm}}) because the permission check resolves the KM by id.
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7-permbase.json" denying role "Internal/subscriber" as "denyKm"
    Then The response status code should be 201
    And The system is ready and I have valid devportal access token as "subscriberUser"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "denyAppPayload"
    And I create an application with payload "denyAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "denyKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{denyKm}}", "grantTypesToBeSupported": ["client_credentials"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "denyKeygenPayload"
    Then The response status code should be 403

  # Pins actual behavior: APIM does NOT enforce the KM's availableGrantTypes at key generation. Requesting a grant
  # the WSO2-IS-7 KM does not list (saml2-bearer) is accepted (200) and echoed back in supportedGrantTypes - the
  # grant list is not validated/filtered against the KM config at this point.
  @rule:grant-types @type:regression
  Scenario: Key generation requesting a grant the WSO2-IS-7 key manager does not list is accepted
    Given The system is ready
    And I have valid access tokens as "admin"
    # "wait until operational": this keygen runs seconds after the KM create, inside the async KM-holder
    # propagation window, where the registration workflow fails with "Key Manager not configured" AFTER
    # inserting the key-mapping row - the leaked row then surfaces as a misleading 901409 on the retry. The
    # waiting create-variant probes keygen against the new KM until the holder has it (validated: without the
    # wait this scenario 409s deterministically; the other keygens sit behind full API create/deploy arcs).
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "gx21Km" and wait until it is operational
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "gx21AppPayload"
    And I create an application with payload "gx21AppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "gx21KeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "{{gx21KmName}}", "grantTypesToBeSupported": ["client_credentials", "urn:ietf:params:oauth:grant-type:saml2-bearer"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "gx21KeygenPayload"
    Then The response status code should be 200
    And The response should contain "urn:ietf:params:oauth:grant-type:saml2-bearer"
