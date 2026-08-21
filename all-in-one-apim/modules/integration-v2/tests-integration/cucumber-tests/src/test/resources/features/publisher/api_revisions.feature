@cleanup
Feature: Publisher API Revisions

  Ports the API-revision CRUD tail from the legacy APIRevisionServerRestartTestCase (functional concern; the
  legacy "restart" was incidental). Beyond create + deploy (already covered across the suite), this exercises
  listing revisions (all and deployed-only), the rule that a deployed revision cannot be deleted (400 until it
  is undeployed), undeploy, delete, and restoring the API's working copy from a revision. Publisher plane, as a
  least-privilege publisher, in BOTH the super tenant and tenant1.com to prove the revision operations are
  tenant-agnostic. Teardown via the per-scenario cleanup hook.

  It also carries the rest of APIRevisionTestCase: that a restore actually REVERTS the working copy, the gateway
  deployment-acknowledgement counts and the relations between them, the invalid-deployment-descriptor rejections,
  every create/delete/deploy/restore/undeploy negative against an id that names nothing (each pinning its exact
  status and error body — including the create-answers-500 asymmetry), the optional-description create, and that
  deleting an API removes its governance-registry artifact collection.

  @cap:publisher @feat:api-lifecycle @type:regression @legacy:APIRevisionServerRestartTestCase
  Scenario Outline: Revision CRUD — list, deploy, delete-while-deployed guard, undeploy, delete, restore as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it

    # Create a second revision and confirm it is listed.
    When I put the following JSON payload in context as "rev2Payload"
    """
    {"description":"second revision"}
    """
    And I make a request to create a revision for "apis" resource "revApiId" with payload "rev2Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "rev2Id"
    When I retrieve the revisions of "apis" resource "revApiId"
    Then The response status code should be 200
    And The response should contain "second revision"
    When I retrieve the deployed revisions of "apis" resource "revApiId"
    Then The response status code should be 200

    # Deploy the second revision.
    When I deploy revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 201
    And I wait until "apis" "revApiId" revision is deployed in the gateway

    # A deployed revision cannot be deleted (400) — it must be undeployed first.
    When I delete revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 400

    # Undeploy, then the delete succeeds (200).
    When I undeploy revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 201
    When I delete revision "rev2Id" of "apis" resource "revApiId"
    Then The response status code should be 200

    # Restore the API's working copy from a fresh revision.
    When I put the following JSON payload in context as "rev3Payload"
    """
    {"description":"restore source revision"}
    """
    And I make a request to create a revision for "apis" resource "revApiId" with payload "rev3Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "rev3Id"
    When I restore revision "rev3Id" of "apis" resource "revApiId"
    Then The response status code should be 201

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Wave B-2: deploying a revision to a vhost NOT configured for the target environment is rejected (400).
  # Ports APIRevisionTestCase.testDeployAPIRevisionWithInvalidVhost.
  @cap:publisher @feat:revisions @rule:invalid-vhost @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deploying a revision to a vhost not configured for the environment is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ivApiId" and deployed it
    When I put the following JSON payload in context as "ivRev"
    """
    {"description":"invalid vhost revision"}
    """
    And I make a request to create a revision for "apis" resource "ivApiId" with payload "ivRev"
    Then The response status code should be 201
    When I put the following JSON payload in context as "ivDeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"ws.wso2.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "ivApiId" with payload "ivDeploy"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # A deployed API revision reports gateway deployment-acknowledgement counts. Ports
  # APIRevisionTestCase.testVerifyDeploymentAcknowledgmentCounts, in full: the deploymentInfo entry carries all
  # three counts, the gateway environment name and a deployedTime, and the counts RELATE consistently —
  # liveGatewayCount > 0, deployedGatewayCount > 0 and deployed + failed <= live. Those relations were previously
  # dropped here as ack-timing flakiness, which left the scenario asserting only that two field NAMES appear in
  # the payload: a gateway-notification path broken end to end reports 0/0/0 and would still have passed. The
  # step waits for the ack (legacy slept 5s then retried for ~100s) instead of asserting into the race.
  @cap:publisher @feat:revisions @rule:deployment-ack @type:regression @legacy:APIRevisionTestCase
  Scenario Outline: A deployed API revision reports gateway deployment-acknowledgement counts as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ackApiId" and deployed it
    When I retrieve the deployed revisions of "apis" resource "ackApiId"
    Then The response status code should be 200
    And The response should contain "{{revisionId}}"
    And The deployment info of revision "revisionId" of "apis" resource "ackApiId" should report acknowledged gateway counts

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Restoring a revision REVERTS the API's working copy — the whole point of restore, and the half the previous
  # scenario left unasserted (it confirmed only the 201). Ports APIRevisionTestCase.testRestoreAPIRevision: a
  # revision is taken, the working copy is then given an extra security scheme (basic_auth), and after restoring
  # that revision the extra scheme is gone. Both states are pinned as EXACT scheme sets, so the assertion fails
  # in both directions — if the restore stops reverting, and if the update stops applying.
  @cap:publisher @feat:revisions @rule:restore @type:regression @legacy:APIRevisionTestCase
  Scenario Outline: Restoring a revision reverts the API configuration as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "restoreApiPayload"
    And I create an "apis" resource with payload "restoreApiPayload" as "restoreApiId"
    Then The response status code should be 201
    And The response field "securityScheme" should be exactly the list "oauth_basic_auth_api_key_mandatory,oauth2"

    # Take the revision that holds the pre-change configuration.
    When I put the following JSON payload in context as "restoreSourceRev"
    """
    {"description":"pre-basic-auth revision"}
    """
    And I make a request to create a revision for "apis" resource "restoreApiId" with payload "restoreSourceRev"
    Then The response status code should be 201
    And I extract response field "id" and store it as "restoreSourceRevId"

    # Change the working copy: add basic_auth to the security schemes.
    When I retrieve the "apis" resource with id "restoreApiId"
    Then The response status code should be 200
    And I put the response payload in context as "restoreApiFull"
    When I update the "apis" resource "restoreApiId" and "restoreApiFull" with configuration type "securityScheme" and value:
      """
      ["oauth_basic_auth_api_key_mandatory", "oauth2", "basic_auth"]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "restoreApiId"
    Then The response field "securityScheme" should be exactly the list "oauth_basic_auth_api_key_mandatory,oauth2,basic_auth"

    # Restore the revision -> the working copy loses basic_auth again.
    When I restore revision "restoreSourceRevId" of "apis" resource "restoreApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "restoreApiId"
    Then The response status code should be 200
    And The response field "securityScheme" should be exactly the list "oauth_basic_auth_api_key_mandatory,oauth2"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # ---------------------------------------------------------------------------------------------------------
  # Revision operations against an id that names nothing. Ports the nine invalid-id negatives of
  # APIRevisionTestCase (create / delete / deploy / restore / undeploy x invalid API UUID / invalid revision
  # UUID), one scenario per operation so each pins its OWN exact status and error body — a grouped scenario
  # would stop at the first mismatch and leave the rest unpinned. The non-existent ids are minted as random
  # UUIDs (well-formed, so nothing can be rejected on id FORMAT before the lookup happens). Each runs in BOTH
  # tenants as a least-privilege publisher (publisherUser and publisherUser@tenant1.com): the subject is how the
  # management API answers a caller-supplied dangling id in either organization.
  #
  # A PRODUCT ASYMMETRY IS PINNED HERE DELIBERATELY, NOT NORMALISED: revision CREATE against a non-existent API
  # answers 500, while delete / deploy / restore / undeploy against a non-existent API all answer 404. One verb
  # answering a server error to the same class of caller-input mistake that four others answer 404 to is a wart
  # worth catching a future fix to, so each code is asserted exactly as observed, with the error body alongside
  # it (the body is what stops a 500 assertion from passing on an unrelated crash). Never widened to 404 || 500.
  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Creating a revision for a non-existent API UUID answers 500 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I generate a random UUID and store it as "noSuchApiId"
    And I put the following JSON payload in context as "orphanRevPayload"
    """
    {"description":"revision of an api that does not exist"}
    """
    When I attempt to create a revision for "apis" resource "noSuchApiId" with payload "orphanRevPayload"
    Then The response status code should be 500
    And The response should contain "Internal server error"
    And The response should contain "Error while adding new API Revision for API : {{noSuchApiId}}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deleting a revision under a non-existent API UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "delNegApiPayload"
    And I create an "apis" resource with payload "delNegApiPayload" as "delNegApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "delNegRevPayload"
    """
    {"description":"delete negative source revision"}
    """
    And I make a request to create a revision for "apis" resource "delNegApiId" with payload "delNegRevPayload"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchApiId"
    When I delete revision "revisionId" of "apis" resource "noSuchApiId"
    Then The response status code should be 404
    And The response should contain "900308"
    And The response should contain "Requested API with id '{{noSuchApiId}}' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deleting a non-existent revision UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "delRevNegApiPayload"
    And I create an "apis" resource with payload "delRevNegApiPayload" as "delRevNegApiId"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchRevisionId"
    When I delete revision "noSuchRevisionId" of "apis" resource "delRevNegApiId"
    Then The response status code should be 404
    And The response should contain "900347"
    And The response should contain "Requested API Revision with id {{noSuchRevisionId}} not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deploying a revision under a non-existent API UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "depNegApiPayload"
    And I create an "apis" resource with payload "depNegApiPayload" as "depNegApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "depNegRevPayload"
    """
    {"description":"deploy negative source revision"}
    """
    And I make a request to create a revision for "apis" resource "depNegApiId" with payload "depNegRevPayload"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchApiId"
    When I put the following JSON payload in context as "depNegDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "noSuchApiId" with payload "depNegDeployPayload"
    Then The response status code should be 404
    And The response should contain "900308"
    And The response should contain "Requested API with id '{{noSuchApiId}}' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deploying a non-existent revision UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "depRevNegApiPayload"
    And I create an "apis" resource with payload "depRevNegApiPayload" as "depRevNegApiId"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchRevisionId"
    When I put the following JSON payload in context as "depRevNegDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "noSuchRevisionId" of "apis" resource "depRevNegApiId" with payload "depRevNegDeployPayload"
    Then The response status code should be 404
    And The response should contain "900347"
    And The response should contain "Requested API Revision with id {{noSuchRevisionId}} not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Restoring a revision under a non-existent API UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "resNegApiPayload"
    And I create an "apis" resource with payload "resNegApiPayload" as "resNegApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "resNegRevPayload"
    """
    {"description":"restore negative source revision"}
    """
    And I make a request to create a revision for "apis" resource "resNegApiId" with payload "resNegRevPayload"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchApiId"
    When I restore revision "revisionId" of "apis" resource "noSuchApiId"
    Then The response status code should be 404
    And The response should contain "900308"
    And The response should contain "Requested API with id '{{noSuchApiId}}' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Restoring a non-existent revision UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "resRevNegApiPayload"
    And I create an "apis" resource with payload "resRevNegApiPayload" as "resRevNegApiId"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchRevisionId"
    When I restore revision "noSuchRevisionId" of "apis" resource "resRevNegApiId"
    Then The response status code should be 404
    And The response should contain "900347"
    And The response should contain "Requested API Revision with id {{noSuchRevisionId}} not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Undeploying a revision under a non-existent API UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "undNegApiPayload"
    And I create an "apis" resource with payload "undNegApiPayload" as "undNegApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "undNegRevPayload"
    """
    {"description":"undeploy negative source revision"}
    """
    And I make a request to create a revision for "apis" resource "undNegApiId" with payload "undNegRevPayload"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchApiId"
    When I undeploy revision "revisionId" of "apis" resource "noSuchApiId"
    Then The response status code should be 404
    And The response should contain "900308"
    And The response should contain "Requested API with id '{{noSuchApiId}}' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:revisions @rule:invalid-id @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Undeploying a non-existent revision UUID is rejected with 404 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "undRevNegApiPayload"
    And I create an "apis" resource with payload "undRevNegApiPayload" as "undRevNegApiId"
    Then The response status code should be 201
    And I generate a random UUID and store it as "noSuchRevisionId"
    When I undeploy revision "noSuchRevisionId" of "apis" resource "undRevNegApiId"
    Then The response status code should be 404
    And The response should contain "900347"
    And The response should contain "Requested API Revision with id {{noSuchRevisionId}} not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Deploying to a gateway environment that is NOT CONFIGURED on the server (an unknown environment NAME) is
  # rejected with 400. Ports APIRevisionTestCase.testDeployAPIRevisionWithInvalidDeploymentInfo. This is a
  # different code path from the invalid-vhost negative above, which names a VALID environment and a vhost that
  # environment does not serve — both are kept: the environment lookup fails here, the vhost lookup there.
  @cap:publisher @feat:revisions @rule:invalid-deployment-info @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Deploying a revision to an unconfigured gateway environment is rejected with 400 as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "badEnvApiPayload"
    And I create an "apis" resource with payload "badEnvApiPayload" as "badEnvApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "badEnvRevPayload"
    """
    {"description":"unconfigured environment revision"}
    """
    And I make a request to create a revision for "apis" resource "badEnvApiId" with payload "badEnvRevPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "badEnvDeployPayload"
    """
    [{"name":"us-region","vhost":"gw.apim.com","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "badEnvApiId" with payload "badEnvDeployPayload"
    Then The response status code should be 400
    And The response should contain "900504"
    # The message double-quotes the environment name ('name 'us-region''): the product interpolates an
    # already-quoted "name 'us-region'" fragment into a template that quotes it again. Pinned verbatim so a
    # tidy-up of the message is noticed rather than silently changing what callers see.
    And The response should contain "Gateway Environment with name 'name 'us-region'' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The UNDEPLOY counterpart of the scenario above, and the test legacy MEANT to write.
  # APIRevisionTestCase.testUnDeployAPIRevisionWithInvalidDeploymentInfo is named for invalid deployment info,
  # but its body passes a VALID environment together with an invalid revision UUID, making it byte-for-byte
  # equivalent to testUnDeployAPIRevisionWithInvalidRevisionUUID (covered above) — so the invalid-deployment-info
  # case on undeploy has never been exercised by anyone. The duplicate is not ported; this is the intended
  # scenario: a revision that IS deployed to the real environment, asked to undeploy from an environment that
  # does not exist.
  @cap:publisher @feat:revisions @rule:invalid-deployment-info @type:negative @legacy:APIRevisionTestCase
  Scenario Outline: Undeploying a revision from an unconfigured gateway environment is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "badEnvUndApiId" and deployed it
    When I put the following JSON payload in context as "badEnvUndeployPayload"
    """
    [{"name":"us-region","vhost":"gw.apim.com","displayOnDevportal":true}]
    """
    And I undeploy revision "revisionId" of "apis" resource "badEnvUndApiId" with payload "badEnvUndeployPayload"
    Then The response status code should be 400
    And The response should contain "900504"
    # The message double-quotes the environment name ('name 'us-region''): the product interpolates an
    # already-quoted "name 'us-region'" fragment into a template that quotes it again. Pinned verbatim so a
    # tidy-up of the message is noticed rather than silently changing what callers see.
    And The response should contain "Gateway Environment with name 'name 'us-region'' not found"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # A revision POST with NO description is accepted (201) — the omitted-optional-field path. Ports
  # APIRevisionTestCase.testCreateAPIRevisionWithoutDescription; every other revision create in this suite sends
  # a description, so the field being genuinely optional was untested. The created revision is then listed, so
  # this cannot pass on a 201 that recorded nothing.
  @cap:publisher @feat:revisions @rule:optional-fields @type:regression @legacy:APIRevisionTestCase
  Scenario Outline: A revision created without a description is accepted as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "noDescApiPayload"
    And I create an "apis" resource with payload "noDescApiPayload" as "noDescApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "noDescRevPayload"
    """
    {}
    """
    And I attempt to create a revision for "apis" resource "noDescApiId" with payload "noDescRevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "noDescRevId"
    When I retrieve the revisions of "apis" resource "noDescApiId"
    Then The response status code should be 200
    And The response should contain "{{noDescRevId}}"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Deleting an API leaves no trace of it in the governance registry: the collection at
  # /_system/governance/apimgt/applicationdata/apis/<apiId> exists while the API does, and the same path faults
  # with the registry's own "Resource does not exist at path" once the API is deleted. Ports
  # APIRevisionTestCase.testIfTracesOfDeletedApisVisible — a real leak check: RegistryPersistenceImpl.deleteAPI
  # removes that collection in a separate "remove revision directory with UUID" step, so it is exactly the kind
  # of cleanup that can silently regress while the API itself disappears from every REST view.
  #
  # That collection is APIConstants.API_REVISION_LOCATION — it holds the API's REVISION artifacts and is created
  # by the FIRST revision, not by the API create (verified: the path faults right after a bare create), which is
  # why a revision is taken here first. Legacy never noticed because its trace check ran last in a class that had
  # created several revisions. There is no REST view of the registry, so the check goes through the Carbon
  # ResourceAdminService SOAP admin service (the interface legacy's ResourceAdminServiceClient wraps) as the
  # acting actor — hence an admin actor rather than the least-privilege publisher user.
  @cap:publisher @feat:api-lifecycle @rule:registry-traces @type:regression @legacy:APIRevisionTestCase
  Scenario Outline: Deleting an API removes its governance registry artifact as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "traceApiPayload"
    And I create an "apis" resource with payload "traceApiPayload" as "traceApiId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "traceRevPayload"
    """
    {"description":"registry trace revision"}
    """
    And I make a request to create a revision for "apis" resource "traceApiId" with payload "traceRevPayload"
    Then The response status code should be 201
    And The governance registry artifact of API "traceApiId" should exist
    When I delete the "apis" resource with id "traceApiId"
    Then The response status code should be 200
    And The governance registry artifact of API "traceApiId" should no longer exist

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
