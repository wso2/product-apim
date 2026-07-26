@cleanup
Feature: Admin Application Search

  Ports ApplicationsSearchByNameOrOwnerTestCase: an admin can search Developer Portal applications via the
  admin API both by name (GET /admin/v4/applications?name=<name>) and by owner (?user=<owner>). An admin-owned
  application is created, then retrieved by each search mode and confirmed present.

  @cap:admin @feat:application-management @type:regression @legacy:ApplicationsSearchByNameOrOwnerTestCase
  Scenario Outline: An admin can search applications by name and by owner as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "searchAppPayload"
    And I create an application with payload "searchAppPayload"
    Then The response status code should be 201
    And I extract response field "name" and store it as "searchAppName"
    # Search by name.
    When I search admin applications by name "{{searchAppName}}"
    Then The response status code should be 200
    And The response should contain "{{searchAppName}}"
    # Search by owner (the acting admin).
    When I search admin applications owned by actor "<actor>"
    Then The response status code should be 200
    And The response should contain "{{searchAppName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
