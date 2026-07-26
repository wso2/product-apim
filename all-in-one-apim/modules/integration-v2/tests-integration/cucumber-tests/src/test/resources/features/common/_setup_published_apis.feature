@setup
Feature: Setup published REST API

  Publishes a REST API in each tenant under test, storing each tenant's published API id under a
  tenant-qualified context key (publishedApiId / publishedApiId@tenant1.com) so consumer scenarios can
  subscribe to the API in the matching tenant. Asserts nothing. Created as the relevant tenant's admin
  (the fixture only needs the tenant, not a specific role). Listed first in the runner; teardown is the
  runner's per-class AfterClass sweep (intentionally untagged so the per-scenario cleanup hook does not
  delete these between scenarios — they must survive across the runner's scenarios).

  Scenario Outline: Publish a REST API in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "publishedApiId<tenantSuffix>" and deployed it
    When I publish the "apis" resource with id "publishedApiId<tenantSuffix>"
    Then The lifecycle status of API "publishedApiId<tenantSuffix>" should be "Published"

    Examples:
      | tenant       | tenantSuffix   |
      | super        |                |
      | tenant1.com  | @tenant1.com   |
