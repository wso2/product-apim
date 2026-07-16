@cleanup
Feature: Admin API Categories

  Admin-plane API category management (/api/am/admin/v4/api-categories): create a category, reject invalid
  creates (no name, special characters, duplicate), update, list, attach to an API and delete. Categories are
  tenant-global, so the category name is uniquely generated and the category is deleted as the scenario's final
  step (with a failure-safe ResourceCleanup backstop). Runs in both the super tenant and tenant1.com as the
  tenant admin. Ports APICategoriesTestCase.

  @cap:admin @feat:api-categories @type:regression @legacy:APICategoriesTestCase
  Scenario Outline: API category create, validation, update, list, attach and delete as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique alphanumeric value and store it as "catName"

    # Create a category
    When I put the following JSON payload in context as "catCreate"
    """
    {"name": "{{catName}}", "description": "Marketing category"}
    """
    And I create an API category with payload "catCreate" as "catId"
    Then The response status code should be 201

    # Negative: missing name is rejected
    When I put the following JSON payload in context as "catNoName"
    """
    {"description": "category without a name"}
    """
    And I attempt to create an API category with payload "catNoName"
    Then The response status code should be 400

    # Negative: a name with special characters is rejected
    When I put the following JSON payload in context as "catSpecial"
    """
    {"name": "Special@Name!#", "description": "bad name"}
    """
    And I attempt to create an API category with payload "catSpecial"
    Then The response status code should be 400

    # Negative: a duplicate name is rejected. Verified live on 4.7.0: the product returns 500 (a known quirk —
    # the unique-constraint violation is not mapped to a 409) with a descriptive body, so we pin the real behaviour.
    When I attempt to create an API category with payload "catCreate"
    Then The response status code should be 500
    And The response should contain "already exists"

    # Update the description
    When I put the following JSON payload in context as "catUpdate"
    """
    {"name": "{{catName}}", "description": "Updated marketing category"}
    """
    And I update the API category "catId" with payload "catUpdate"
    Then The response status code should be 200
    And The response should contain "Updated marketing category"

    # List categories — our category is present
    When I retrieve all API categories
    Then The response status code should be 200
    And The response should contain "{{catName}}"

    # Attach the category to an API — the API then carries the category name
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "catApiPayload"
    And I replace "\"additionalProperties\": []" with "\"categories\": [\"{{catName}}\"], \"additionalProperties\": []" in the payload "catApiPayload"
    And I create an "apis" resource with payload "catApiPayload" as "catApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "catApiId"
    Then The response status code should be 200
    And The response should contain "{{catName}}"

    # Delete the category (still referenced by the API — APIM detaches it and returns 200)
    When I delete the API category "catId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
