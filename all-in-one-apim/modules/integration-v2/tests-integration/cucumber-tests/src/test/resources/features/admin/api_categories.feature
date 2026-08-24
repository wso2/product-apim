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

    # Negative: a name containing a SPACE is rejected. This is a DIFFERENT rule from the punctuation arm above —
    # a space is not punctuation, and legacy's APICategoriesTestCase pins exactly this case ("Marketing Category"),
    # so asserting punctuation alone would leave the space rule unpinned.
    When I put the following JSON payload in context as "catSpaced"
    """
    {"name": "Marketing Category", "description": "This is Marketing Category"}
    """
    And I attempt to create an API category with payload "catSpaced"
    Then The response status code should be 400

    # Negative: a duplicate name is rejected. Verified live on 4.7.0: the product returns 500 (a known quirk —
    # the unique-constraint violation is not mapped to a 409) with a descriptive body, so we pin the real behaviour.
    When I attempt to create an API category with payload "catCreate"
    Then The response status code should be 500
    # Exact composed message, not the bare "already exists" fragment: the REST layer wraps the cause as
    # "Error while adding new API Category '<name>' - " + e.getMessage(). Naming the category proves the
    # rejection is about THIS name, not some other collision.
    And The response should contain "Error while adding new API Category '{{catName}}' - Category with name '{{catName}}' already exists"

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

    # Delete the attached API FIRST so the category delete is deterministic. Deleting a STILL-attached category
    # is non-deterministic under load — the product auto-detaches and returns 200 usually, but under full-suite
    # load sometimes 500s "Unable to delete the category. It is attached to API(s)" (flagged as an upstream
    # inconsistency; not pinned here to avoid a flaky assertion).
    When I delete the "apis" resource with id "catApiId"
    Then The response status code should be 200
    When I delete the API category "catId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A category attached to an API PRODUCT (rather than an API) and carried through publish onto the product's
  # DevPortal representation. Ports APIProductCreationTestCase.testCreateAndInvokeApiProductWithAPICategoryAdded,
  # which never ran (its @Test annotation is commented out) — implemented here because a silenced test is not
  # evidence the scenario is invalid, and nothing else attaches a category to a product.
  # Lives with the other api-categories coverage rather than in publisher/api_products.feature: the subject is the
  # CATEGORY's attachability and propagation (@cap:admin @feat:api-categories), the same subject as the
  # attach-to-an-API leg above, with the product being only the attach target. The product create/publish and the
  # DevPortal read are prerequisites, hence @dep:publisher / @dep:devportal.
  # Assertions are exact-set equality on the categories array (not a body substring): the property is that the
  # product carries EXACTLY the attached category, so an extra or dropped element must fail.
  # Legacy also created a revision and deployed the product before publishing; no assertion depended on the
  # deployment (it asserted no invocation despite its name), so the deploy arc is omitted — the category is
  # publisher/DevPortal metadata, not a gateway artifact.
  # Teardown: no inline deletes. The cleanup hook sweeps categories LAST, after the product and then the API, so
  # the category is already detached when it is deleted — the "deleting a still-attached category is
  # non-deterministic" hazard noted on the scenario above cannot arise here.
  @cap:admin @feat:api-categories @rule:product-attach @type:regression @dep:publisher @dep:devportal @legacy:APIProductCreationTestCase
  Scenario Outline: An API category attached to an API product survives publish and reaches the devportal as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I generate a unique alphanumeric value and store it as "prodCatName"
    When I put the following JSON payload in context as "prodCatCreate"
    """
    {"name": "{{prodCatName}}", "description": "Category attached to an API product"}
    """
    And I create an API category with payload "prodCatCreate" as "prodCatId"
    Then The response status code should be 201

    # An API product over one API — the attach target.
    Given I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodCatApiId" and deployed it
    When I create an API product "${UNIQUE:CatProduct}" with context "${UNIQUE:catProductCtx}" from API "prodCatApiId" as "prodCatProductId"
    Then The response status code should be 201

    # Attach the category to the PRODUCT (GET → set categories → PUT); the update echoes it back.
    When I retrieve the "api-products" resource with id "prodCatProductId"
    Then The response status code should be 200
    And I put the response payload in context as "prodCatProductPayload"
    When I update the "api-products" resource "prodCatProductId" and "prodCatProductPayload" with configuration type "categories" and value:
      """
      ["{{prodCatName}}"]
      """
    Then The response status code should be 200
    And The response field "categories" should be exactly the list "{{prodCatName}}"

    # Present on the product BEFORE publish.
    When I retrieve the "api-products" resource with id "prodCatProductId"
    Then The response status code should be 200
    And The response field "categories" should be exactly the list "{{prodCatName}}"

    # Still present AFTER publish.
    When I publish the "api-products" resource with id "prodCatProductId"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "prodCatProductId"
    Then The response status code should be 200
    And The response field "categories" should be exactly the list "{{prodCatName}}"

    # And on the product's DevPortal representation (the devportal exposes products under /apis/{id}). Polled:
    # devportal visibility after publish is eventually consistent, so a single GET can read stale state.
    When I retrieve the devportal API "prodCatProductId" until it contains "{{prodCatName}}" within 60 seconds
    Then The response status code should be 200
    And The response field "categories" should be exactly the list "{{prodCatName}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
