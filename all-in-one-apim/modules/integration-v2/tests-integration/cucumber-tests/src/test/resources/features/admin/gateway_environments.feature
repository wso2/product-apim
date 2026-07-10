@cleanup
Feature: Admin Gateway Environments

  Ports the core of the legacy EnvironmentTestCase: admin-plane CRUD of gateway environments via the admin REST
  API — create (with a vhost; and a specific gateway type), list, retrieve, update, delete, the not-found (404)
  edge, and the create-validation negatives (no vhost, invalid name, no display name, already-existing). CRUD
  runs ×2 tenant (tenant admins manage their own environments); the gatewayType + negative scenarios run super.
  Each scenario uses uniquely-named environments (parallel-safe) and cleans them up. Gateway permissions (create
  env with an ALLOW role permission -> persists) are covered below; the deploy-time enforcement half was
  commented-out in legacy and stays deferred. Deferred to increment 2: multiple/special-char vhost variants,
  deploy-a-revision-to-a-vhost, delete-env-with-deployed-revisions, devportal-swagger validation, and
  get-instances of the default environment.

  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: Gateway environment CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment with name "${UNIQUE:envCrud}" display name "Env CRUD" and vhost host "crud.gw.example.com"
    Then The response status code should be 201
    When I retrieve all gateway environments
    Then The response status code should be 200
    And The response should contain "crud.gw.example.com"
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    When I update the gateway environment "environmentId" setting its description to "updated environment description"
    Then The response status code should be 200
    And The response should contain "updated environment description"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario: Create a gateway environment with a specific gateway type
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a gateway environment "${UNIQUE:envApk}" with vhost host "apk.gw.example.com" and gateway type "APK"
    Then The response status code should be 201
    And The response should contain "APK"

  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario: Invalid gateway environment creations are rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    # No vhost → 400.
    When I create a gateway environment with name "${UNIQUE:envNoVhost}" display name "No Vhost" and vhost host ""
    Then The response status code should be 400
    # Invalid (special-character) name → 400.
    When I create a gateway environment with name "inv@lid env name!" display name "Bad Name" and vhost host "bad.gw.example.com"
    Then The response status code should be 400
    # Missing display name → 400.
    When I create a gateway environment with name "${UNIQUE:envNoDisplay}" display name "" and vhost host "nodisplay.gw.example.com"
    Then The response status code should be 400
    # Already-existing environment (the built-in Default) → 400.
    When I create a gateway environment with name "Default" display name "Default" and vhost host "default.gw.example.com"
    Then The response status code should be 400

  # Gateway-environment permissions (increment-2 Group B) — an environment can be created with a role permission
  # (ALLOW), and it persists. Salvages the legacy (commented-out) testGatewayPermissions as an admin-plane CRUD
  # assertion; the deploy-time enforcement half (an out-of-role user cannot deploy to the env) is the disabled,
  # unverified part and stays deferred. ×2 tenant. Ports EnvironmentTestCase#testGatewayPermissions (CRUD half).
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment can be created with a role permission as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envPerm}" with vhost host "perm.gw.example.com" allowing role "Internal/creator"
    Then The response status code should be 201
    When I retrieve the gateway environment with id "environmentId"
    Then The response status code should be 200
    And The response should contain "Internal/creator"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E6: the built-in Default environment reports its registered gateway instances. The one legacy-ENABLED item
  # of the deferred set (testGetGatewayInstancesInDefaultEnvironment).
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: The Default gateway environment reports its gateway instances as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the gateway instances of environment "Default"
    Then The response status code should be 200
    And The response should contain "count"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E2: duplicate-hostname vhost negative — NEW verified coverage (legacy-disabled). Two vhosts with the same
  # hostname within one environment are rejected (400).
  # verify-first FINDING: the legacy-disabled "special-character vhost hostname" case (e.g. "foods.com#$%?") is
  # NOT rejected on 4.7.0 — the product ACCEPTS it (201). So that assertion (never run in legacy CI) does not
  # reflect current behaviour and is deliberately NOT ported as a negative.
  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment with duplicate vhost hostnames is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envDupVhost}" with vhost hosts "dup.gw.example.com,dup.gw.example.com"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E1: an environment can be created with MULTIPLE vhosts, and updated to remove one — NEW verified coverage
  # (legacy-disabled testAddGatewayEnvironmentMultipleVHosts / testUpdateEnvironmentByRemovingVHost). ×2 tenant.
  @cap:admin @feat:environments @type:regression @legacy:EnvironmentTestCase
  Scenario Outline: A gateway environment with multiple vhosts can be created and have a vhost removed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a gateway environment "${UNIQUE:envMultiVhost}" with vhost hosts "one.gw.example.com,two.gw.example.com"
    Then The response status code should be 201
    And The response should contain "one.gw.example.com"
    And The response should contain "two.gw.example.com"
    When I update the gateway environment "environmentId" to only vhost host "one.gw.example.com"
    Then The response status code should be 200
    And The response should not contain "two.gw.example.com"
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E3 + E4: an API revision can be deployed to a custom environment's vhost, and the environment can only be
  # deleted after the revision is undeployed (a still-deployed environment → 409). NEW verified coverage
  # (legacy-disabled testDeployApiRevisionWithVhost / testDeleteEnvironmentAfterUndeployingRevisions). The env
  # is addressed by NAME in the deploy payload but by ID (a UUID) for delete.
  @cap:admin @feat:environments @type:regression @dep:publisher @legacy:EnvironmentTestCase
  Scenario Outline: An API revision deploys to a custom environment's vhost; the environment deletes only after undeploy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e3ApiPayload"
    And I create an "apis" resource with payload "e3ApiPayload" as "e3ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e3Env}" with vhost hosts "e3.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e3EnvName"
    When I put the following JSON payload in context as "e3RevPayload"
    """
    {"description":"revision for vhost deploy"}
    """
    And I make a request to create a revision for "apis" resource "e3ApiId" with payload "e3RevPayload"
    Then The response status code should be 201
    # Deploy the revision to the custom environment's vhost.
    When I put the following JSON payload in context as "e3DeployPayload"
    """
    [{"name":"{{e3EnvName}}","vhost":"e3.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "e3ApiId" with payload "e3DeployPayload"
    Then The response status code should be 201
    # The environment cannot be deleted while it still has a deployed revision.
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 409
    # After undeploying from the custom environment, it can be deleted.
    When I put the following JSON payload in context as "e3UndeployPayload"
    """
    [{"name":"{{e3EnvName}}","vhost":"e3.gw.example.com","displayOnDevportal":true}]
    """
    And I undeploy revision "revisionId" of "apis" resource "e3ApiId" with payload "e3UndeployPayload"
    Then The response status code should be 201
    When I delete the gateway environment with id "environmentId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E4 (edges): deleting the built-in Default environment, or a non-existent one, is rejected.
  @cap:admin @feat:environments @type:negative @legacy:EnvironmentTestCase
  Scenario Outline: Deleting the built-in Default environment or a non-existent environment is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I delete the gateway environment with id "Default"
    Then The response status code should be 400
    When I delete the gateway environment with id "nonexistent-environment-00000"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # E5: an API deployed to a custom environment reflects that environment's vhost in its Developer Portal
  # endpoint URLs. NEW verified coverage (legacy-disabled testValidateDevportalAPIAndSwaggerResponse).
  @cap:devportal @feat:discovery @rule:environment @type:regression @dep:publisher @legacy:EnvironmentTestCase
  Scenario Outline: A devportal API reflects the custom environment vhost it is deployed to as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "e5ApiPayload"
    And I create an "apis" resource with payload "e5ApiPayload" as "e5ApiId"
    Then The response status code should be 201
    When I create a gateway environment "${UNIQUE:e5Env}" with vhost hosts "e5.gw.example.com"
    Then The response status code should be 201
    And I extract response field "name" and store it as "e5EnvName"
    When I put the following JSON payload in context as "e5RevPayload"
    """
    {"description":"revision for devportal env check"}
    """
    And I make a request to create a revision for "apis" resource "e5ApiId" with payload "e5RevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "e5DeployPayload"
    """
    [{"name":"{{e5EnvName}}","vhost":"e5.gw.example.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "e5ApiId" with payload "e5DeployPayload"
    Then The response status code should be 201
    When I publish the "apis" resource with id "e5ApiId"
    Then The lifecycle status of API "e5ApiId" should be "Published"
    # The devportal view of the API carries the custom environment's vhost among its endpoint URLs.
    When I retrieve the devportal API "e5ApiId" until it contains "e5.gw.example.com" within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
