@cleanup
Feature: Admin Throttling Policy Export / Import

  Ports ThrottlePolicyExportImportTestCase: exporting a throttling policy and re-importing it. For each policy
  type the arc is the same — create the policy, export it (200), assert the exported artifact's CONTENT (the
  envelope's type/subtype/APIM version and the policy DTO inside it, which is what legacy deep-equalled), then
  import that artifact three ways: without overwrite while it still exists (409 conflict), with overwrite (200
  update), and — after deleting the policy — without overwrite again (201 new).
  subscription/application/advanced run ×2 tenant. Custom (Siddhi) policies are SUPER-TENANT ONLY: every custom
  operation, export included, is guarded by ThrottlingApiServiceImpl#checkTenantDomainForCustomRules, so the
  custom round trip is super-tenant only and the tenant-admin export refusal (403) is pinned by its own scenario.
  Legacy threw a SkipException for the custom cases in tenant mode — that guard is exactly WHY it skipped, so the
  refusal is a real unported negative rather than a non-item. The advanced/application/subscription exports are
  NOT guarded, which is why legacy ran those in both tenant modes; they stay positive in both.
  The final re-imported (same-named, unique) policy is left to the block's container teardown.

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import a subscription throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "eiSub${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "subscription" throttling policy named "subThrottlePolicyName" as "eiSubExport"
    Then The response status code should be 200
    And The value of response field "type" should be "throttling policy"
    And The value of response field "subtype" should be "subscription policy"
    And The value of response field "version" should be "v4.7.0"
    And The value of response field "data.policyName" should be "{{subThrottlePolicyName}}"
    And The value of response field "data.type" should be "SubscriptionThrottlePolicy"
    And The value of response field "data.isDeployed" should be "false"
    And The value of response field "data.defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "data.defaultLimit.requestCount.requestCount" should be "1000"
    And The value of response field "data.defaultLimit.requestCount.timeUnit" should be "min"
    And The value of response field "data.defaultLimit.requestCount.unitTime" should be "1"
    And The value of response field "data.billingPlan" should be "FREE"
    And The value of response field "data.stopOnQuotaReach" should be "true"
    And The value of response field "data.permissions.permissionType" should be "ALLOW"
    And The response field "data.permissions.roles" should be exactly the list "Internal/everyone"
    When I import the throttling policy from "eiSubExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiSubExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiSubExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import an application throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "eiApp${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "application" throttling policy named "appThrottlePolicyName" as "eiAppExport"
    Then The response status code should be 200
    And The value of response field "type" should be "throttling policy"
    And The value of response field "subtype" should be "application policy"
    And The value of response field "version" should be "v4.7.0"
    And The value of response field "data.policyName" should be "{{appThrottlePolicyName}}"
    And The value of response field "data.type" should be "ApplicationThrottlePolicy"
    And The value of response field "data.isDeployed" should be "false"
    And The value of response field "data.defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "data.defaultLimit.requestCount.requestCount" should be "1000"
    And The value of response field "data.defaultLimit.requestCount.timeUnit" should be "min"
    And The value of response field "data.defaultLimit.requestCount.unitTime" should be "1"
    When I import the throttling policy from "eiAppExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiAppExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiAppExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: Export and re-import an advanced throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "eiAdv${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "advanced" throttling policy named "advThrottlePolicyName" as "eiAdvExport"
    Then The response status code should be 200
    And The value of response field "type" should be "throttling policy"
    And The value of response field "subtype" should be "advanced policy"
    And The value of response field "version" should be "v4.7.0"
    And The value of response field "data.policyName" should be "{{advThrottlePolicyName}}"
    And The value of response field "data.type" should be "AdvancedThrottlePolicy"
    And The value of response field "data.isDeployed" should be "false"
    And The value of response field "data.defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "data.defaultLimit.requestCount.requestCount" should be "1000"
    And The value of response field "data.defaultLimit.requestCount.timeUnit" should be "min"
    And The value of response field "data.defaultLimit.requestCount.unitTime" should be "1"
    And The response field "data.conditionalGroups[*].description" should be exactly the list ""
    When I import the throttling policy from "eiAdvExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiAdvExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiAdvExport" with overwrite "false"
    Then The response status code should be 201

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Custom (Siddhi) throttling policies are an admin-global concern — every custom operation is super-tenant only
  # — so this is super-tenant only (×1), consistent with the custom-policy CRUD coverage.
  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario: Export and re-import a custom throttling policy
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "eiCustom${UNIQUE:Pol}" throttling API context "/eiCtx${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 201
    When I export the "custom" throttling policy named "customThrottlePolicyName" as "eiCustomExport"
    Then The response status code should be 200
    And The value of response field "type" should be "throttling policy"
    And The value of response field "subtype" should be "custom rule"
    And The value of response field "version" should be "v4.7.0"
    And The value of response field "data.policyName" should be "{{customThrottlePolicyName}}"
    And The value of response field "data.type" should be "CustomRule"
    And The value of response field "data.isDeployed" should be "false"
    And The value of response field "data.keyTemplate" should be "$apiContext"
    And The response should contain "INSERT INTO EligibilityStream"
    When I import the throttling policy from "eiCustomExport" with overwrite "false"
    Then The response status code should be 409
    When I import the throttling policy from "eiCustomExport" with overwrite "true"
    Then The response status code should be 200
    When I delete the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    When I import the throttling policy from "eiCustomExport" with overwrite "false"
    Then The response status code should be 201

  # Exporting a CUSTOM policy as a tenant admin is refused 403 by the super-tenant guard on the export path's
  # custom branch (ThrottlingApiServiceImpl#exportThrottlingPolicy → checkTenantDomainForCustomRules) — a valid
  # tenant admin token carrying apim:admin, so this is NOT the 401 a missing scope produces. Legacy skipped this
  # case in tenant mode (SkipException) and therefore never asserted it. The policy is created by the SUPER-tenant
  # admin first so the refusal is unambiguously the tenant guard and not a missing policy (404).
  @cap:admin @feat:throttling-policies @type:negative @legacy:ThrottlePolicyExportImportTestCase
  Scenario: Exporting a custom throttling policy is refused to a tenant admin
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "eiTenantCustom${UNIQUE:Pol}" throttling API context "/eiTc${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 201
    And I have valid access tokens as "admin@tenant1.com"
    When I export the "custom" throttling policy named "customThrottlePolicyName" as "eiTenantCustomExport"
    Then The response status code should be 403

  # The advanced/application/subscription export paths are NOT tenant-guarded, which is what makes the custom 403
  # above specific to custom rules rather than a blanket tenant restriction on export.
  @cap:admin @feat:throttling-policies @type:regression @legacy:ThrottlePolicyExportImportTestCase
  Scenario Outline: An admin can export a non-custom throttling policy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "eiTenantAdv${UNIQUE:Pol}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I export the "advanced" throttling policy named "advThrottlePolicyName" as "eiTenantAdvExport"
    Then The response status code should be 200
    And The value of response field "subtype" should be "advanced policy"
    And The value of response field "data.policyName" should be "{{advThrottlePolicyName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
