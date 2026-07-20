@cleanup
Feature: Admin Platform Gateway Management

  Admin-plane CRUD of platform (Universal / self-hosted) gateways via the admin REST API
  (POST/GET/PUT/DELETE /gateways, regenerate-token), the internal
  control-plane surface a connected gateway calls (the WebSocket registration handshake, GET
  /internal/data/v1/deployments, POST .../deployments/fetch-batch, GET /internal/gateway/.well-known), and how a
  platform gateway surfaces as a GET /environments/{id} entry (gatewayType APIPlatform) while staying excluded
  from the plain GET /environments list by name. A platform gateway is still a gateway environment, so this
  stays under @feat:environments rather than a new feature — it just has its own REST surface and lifecycle
  (registration token, WebSocket connect) instead of a vhost/service_url pair. Tenant-agnostic negatives (a
  missing/invalid api-key, the well-known payload, the since/unknown-ids/empty-list fetch-batch edges) run once
  rather than doubled — the assertion does not depend on organization scoping.

  @cap:admin @feat:environments @type:regression
  Scenario Outline: Registering a platform gateway makes it appear in the gateways list as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwcreate}"
    Then The response status code should be 201
    And The response should contain "registrationToken"
    When I retrieve all platform gateways
    Then The response status code should be 200
    And The response should contain "{{platformGatewayName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression
  Scenario Outline: Updating a platform gateway changes its display name as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwupdate}"
    Then The response status code should be 201
    When I update the platform gateway "platformGatewayId" setting its display name to "Updated display" and description to "updated"
    Then The response status code should be 200
    And The response should contain "Updated display"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression
  Scenario Outline: Regenerating a platform gateway's token revokes the old one as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwregen}"
    Then The response status code should be 201
    When I regenerate the token for the platform gateway "platformGatewayId"
    Then The response status code should be 200
    And The response should contain "registrationToken"
    When I retrieve internal deployments using api key "platformGatewayOldToken"
    Then The response status code should be 401

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression
  Scenario Outline: Deleting a platform gateway then deleting it again returns 404 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwdelete}"
    Then The response status code should be 201
    When I delete the platform gateway with id "platformGatewayId"
    Then The response status code should be 200
    When I delete the platform gateway with id "platformGatewayId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative
  Scenario Outline: Registering a platform gateway with a duplicate name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwdup}"
    Then The response status code should be 201
    When I register a platform gateway named "{{platformGatewayName}}"
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative
  Scenario Outline: Registering a platform gateway with an invalid name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "<invalidName>"
    Then The response status code should be 400

    Examples:
      | actor | invalidName                                                      |
      | admin | ab                                                               |
      | admin | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa |
      | admin | Invalid_Name                                                     |
      | admin | bad name                                                         |
      | admin | gateway!@#                                                       |
      | admin@tenant1.com | ab                                                        |
      | admin@tenant1.com | aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa |
      | admin@tenant1.com | Invalid_Name                                              |
      | admin@tenant1.com | bad name                                                  |
      | admin@tenant1.com | gateway!@#                                                |

  @cap:admin @feat:environments @type:negative
  Scenario Outline: Updating or regenerating the token of an unknown platform gateway returns 404 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I update the platform gateway "nonexistent-platform-gateway-00000" setting its display name to "Display" and description to "desc"
    Then The response status code should be 404
    When I regenerate the token for the platform gateway "nonexistent-platform-gateway-00000"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PUT keeps the gateway `name` immutable (GatewaysApiServiceImpl#updatePlatformGateway rejects a body name
  # that does not match the existing gateway), and rejects an invalid vhost.
  @cap:admin @feat:environments @type:negative
  Scenario Outline: PUT cannot rename a platform gateway or set an invalid vhost as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwput}"
    Then The response status code should be 201
    When I update the platform gateway "platformGatewayId" with name "some-other-name"
    Then The response status code should be 400
    When I update the platform gateway "platformGatewayId" with vhost "not-a-valid-url"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression @dep:publisher @dep:devportal
  Scenario: A DENY-role permission hides the platform gateway from the restricted role's devportal view
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And The system is ready and I have valid devportal access token as "subscriberUser"
    And I have valid access tokens as "admin"
    And I act as "admin"
    When I register a platform gateway named "${UNIQUE:pgwdeny}" denying role "Internal/subscriber"
    Then The response status code should be 201
    And I act as "publisherUser"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwDenyApiPayload"
    And I create an "apis" resource with payload "pgwDenyApiPayload" as "pgwDenyApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "pgwDenyApiId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "pgwDenyRevPayload"
    """
    {"description":"platform gateway deny permission enforcement"}
    """
    And I make a request to create a revision for "apis" resource "pgwDenyApiId" with payload "pgwDenyRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwDenyDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwDenyApiId" with payload "pgwDenyDeployPayload"
    Then The response status code should be 201
    And I act as "subscriberUser"
    When I retrieve the devportal API "pgwDenyApiId"
    Then The response status code should be 200
    And The response should not contain "{{platformGatewayName}}"

  @cap:admin @feat:environments @type:regression @dep:publisher @dep:devportal
  Scenario: An ALLOW-role permission scopes the platform gateway's visibility to only that role
    Given The system is ready and I have valid publisher access tokens as "publisherUser"
    And The system is ready and I have valid devportal access token as "subscriberUser"
    And I have valid access tokens as "admin"
    And I act as "admin"
    When I register a platform gateway named "${UNIQUE:pgwallow}" allowing role "Internal/publisher"
    Then The response status code should be 201
    And I act as "publisherUser"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "pgwAllowApiPayload"
    And I create an "apis" resource with payload "pgwAllowApiPayload" as "pgwAllowApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "pgwAllowApiId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "pgwAllowRevPayload"
    """
    {"description":"platform gateway allow permission enforcement"}
    """
    And I make a request to create a revision for "apis" resource "pgwAllowApiId" with payload "pgwAllowRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pgwAllowDeployPayload"
    """
    [{"name":"{{platformGatewayName}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "pgwAllowApiId" with payload "pgwAllowDeployPayload"
    Then The response status code should be 201
    And I act as "subscriberUser"
    When I retrieve the devportal API "pgwAllowApiId"
    Then The response status code should be 200
    And The response should not contain "{{platformGatewayName}}"
    And I act as "publisherUser"
    When I retrieve the devportal API "pgwAllowApiId"
    Then The response status code should be 200
    And The response should contain "{{platformGatewayName}}"

  # A platform gateway created under one tenant must not be visible to the other tenant's admin — CRUD (create,
  # then a cross-tenant GET) rather than a Scenario Outline over a single actor, since it needs BOTH tenants'
  # tokens live in the same scenario.
  @cap:admin @feat:environments @type:negative
  Scenario Outline: A platform gateway is invisible to the other tenant's admin as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have valid access tokens as "<otherActor>"
    And I act as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwxt}"
    Then The response status code should be 201
    And I act as "<otherActor>"
    When I retrieve the gateway environment with literal or context id "platformGatewayId"
    Then The response status code should be 404

    Examples:
      | actor             | otherActor        |
      | admin              | admin@tenant1.com |
      | admin@tenant1.com  | admin             |

  @cap:admin @feat:environments @type:regression
  Scenario Outline: A registered platform gateway is retrievable as an environment, inactive, before WS connect as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwinactive}"
    Then The response status code should be 201
    When I retrieve the gateway environment with id "platformGatewayId"
    Then The response status code should be 200
    And The response should contain "APIPlatform"
    And The platform gateway "platformGatewayId" should become inactive within 5 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression
  Scenario Outline: WebSocket connect and disconnect flip the platform gateway's active state as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwactive}"
    Then The response status code should be 201
    When I connect to the internal gateway control-plane WebSocket using api key "platformGatewayToken"
    Then The internal gateway WebSocket connection should be open
    And The platform gateway "platformGatewayId" should become active within 15 seconds
    When I close the internal gateway WebSocket connection
    Then The platform gateway "platformGatewayId" should become inactive within 15 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative
  Scenario: An invalid registration token is rejected at the internal WebSocket connect
    Given The system is ready
    When I connect to the internal gateway control-plane WebSocket using api key "definitely-not-a-valid-platform-gateway-token"
    Then The internal gateway WebSocket connection should be rejected with close code 4401

  @cap:admin @feat:environments @type:regression
  Scenario Outline: Internal /deployments requires a valid api-key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I register a platform gateway named "${UNIQUE:pgwdeployments}"
    Then The response status code should be 201
    When I retrieve internal deployments using api key "platformGatewayToken"
    Then The response status code should be 200
    And The response should contain "deployments"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:negative
  Scenario: Internal /deployments without an api-key is rejected
    Given The system is ready
    When I retrieve internal deployments without an api key
    Then The response status code should be 401

  @cap:admin @feat:environments @type:regression
  Scenario: The internal gateway well-known endpoint exposes the discovery payload
    Given The system is ready
    When I retrieve the internal gateway well-known discovery document
    Then The response status code should be 200
    And The response should contain "internal/data/v1"
    And The response should contain "controlPlane"
    And The response should contain "APIM"

  @cap:admin @feat:environments @type:negative
  Scenario Outline: GET /environments/{unknownId} returns 404 and excludes registered platform gateways by name as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the gateway environment with literal or context id "nonexistent-environment-00000"
    Then The response status code should be 404
    When I register a platform gateway named "${UNIQUE:pgwexclude}"
    Then The response status code should be 201
    When I retrieve all gateway environments
    Then The response status code should be 200
    And The response should not contain "{{platformGatewayName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression
  Scenario: Internal /deployments accepts an optional since filter, and fetch-batch tolerates unknown/empty ids
    Given The system is ready
    And I have valid access tokens as "admin"
    When I register a platform gateway named "${UNIQUE:pgwsince}"
    Then The response status code should be 201
    When I retrieve internal deployments with since "1970-01-01T00:00:00Z" using api key "platformGatewayToken"
    Then The response status code should be 200
    And The response should contain "deployments"
    When I fetch internal deployment batch for deployment ids "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee,bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" using api key "platformGatewayToken"
    Then The response status code should be 200
    When I fetch internal deployment batch for deployment ids "" using api key "platformGatewayToken"
    Then The response status code should be 400
