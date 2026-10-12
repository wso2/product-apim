@cleanup
Feature: Publisher API Products

  Ports the core of the legacy APIProductCreationTestCase + APIProductRevisionTestCase + the publisher half of
  APIProductLifecycleTest: an API Product aggregates selected resources of SEVERAL existing APIs into a new entity
  with its own context/version, taken through revision and lifecycle. Products here aggregate TWO APIs with
  disjoint resource sets (the shape every legacy product test used), since a one-API product exercises neither the
  multi-member aggregation nor the per-member operation sets. This feature covers the publisher plane — create +
  verify the aggregation and the publisher listing fidelity, re-validate the product's own generated definition
  through the OAS validator, reject a malformed context, create a new version (incl. as default), confirm the
  product tracks its underlying API through a re-save, mutual-SSL security + a client certificate on a PRODUCT,
  and the product revision lifecycle (reusing the generic revision steps with resourceType "api-products").
  Gateway invocation of a product is covered by gateway/api_product_invocation. ×2 tenant where the concern is
  tenant-agnostic. Legacy's SUPER_TENANT_USER_STORE_USER factory mode is ported as a third actor dimension:
  SECONDARY.COM store actors drive the create arc and a dedicated full-lifecycle scenario, which is why this
  feature's block sets initSecondaryUserStore. Teardown via @cleanup — the product is deleted before its
  underlying APIs (a product references the APIs).

  @cap:publisher @feat:products @type:smoke @dep:publisher @legacy:APIProductCreationTestCase @legacy:APIProductLifecycleTest
  Scenario Outline: Create an API product aggregating two APIs in CREATED state as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "prodApiTwoId" and deployed it
    When I create an API product "${UNIQUE:AggProduct}" with context "${UNIQUE:aggProductCtx}" from APIs "prodApiId,prodApiTwoId" as "productId"
    Then The response status code should be 201
    # The create response echoes the request: name, version, context (tenant-prefixed for a tenant provider) and
    # the creator as provider — also for a store-qualified creator.
    And The create response of API product "productId" should echo its name, context, version and the provider of actor "<actor>"
    # A freshly created product sits in CREATED (it is published by a separate lifecycle transition).
    And The value of response field "state" should be "CREATED"
    # Both member APIs are aggregated, each contributing its own resource set.
    And The response array field "apis" should have exactly 2 entries
    And The response field "apis[*].apiId" should be exactly the list "{{prodApiId}},{{prodApiTwoId}}"
    # The product retrieve echoes both aggregated API ids, and its swagger carries both resource paths.
    When I retrieve the "api-products" resource with id "productId"
    Then The response status code should be 200
    And The response should contain "{{prodApiId}}"
    And The response should contain "{{prodApiTwoId}}"
    When I retrieve the API product swagger of "productId"
    Then The response status code should be 200
    And The response should contain "/customers/{id}"
    And The response should contain "/assets"
    # Publisher listing fidelity: the product is listed exactly once and the listing agrees with its own GET.
    Then The publisher product list should report API product "productId" exactly once with the same info fields
    # The product read back by id is the product that was created, field by field.
    And The API product "productId" read from the publisher should match its create response

    # The last two rows are legacy's SUPER_TENANT_USER_STORE_USER factory mode: the creator lives in the
    # SECONDARY.COM JDBC user store, so its username is store-qualified and the product records a provider derived
    # from it. Same assertions — the point is that none of them changes for a store-resident creator.
    Examples:
      | actor                                    |
      | publisherUser                            |
      | publisherUser@tenant1.com                |
      | SECONDARY.COM/publisherUser1             |
      | SECONDARY.COM/publisherUser1@tenant1.com |

  # The legacy testAPIProductSwaggerDefinition's actual subject: the definition a product GENERATES must itself be
  # a valid OpenAPI document, proven by feeding it back through the publisher OAS validation endpoint.
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product's generated definition is a valid OpenAPI document as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "swaggerApiOneId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "swaggerApiTwoId" and deployed it
    When I create an API product "${UNIQUE:SwaggerProduct}" with context "${UNIQUE:swaggerProductCtx}" from APIs "swaggerApiOneId,swaggerApiTwoId" as "swaggerProductId"
    Then The response status code should be 201
    When I retrieve the API product swagger of "swaggerProductId"
    Then The response status code should be 200
    And I put the response payload in context as "swaggerProductDefinition"
    When I validate the openapi definition captured as "swaggerProductDefinition"
    Then The response status code should be 200
    And The value of response field "isValid" should be "true"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # The member API is IMPORTED from legacy's OpenAPI definition whose operations reference components/parameters,
  # components/headers and a PATH-LEVEL parameter through $ref. The product is deployed and published, and the
  # definition it generates from that member must still be a valid OpenAPI document — the reference-resolution
  # case testAPIProductSwaggerDefinition exists for.
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product over a member imported from a $ref-bearing OpenAPI definition generates a valid definition as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import openapi definition from "artifacts/payloads/api_product_ref_member_oas.yaml" with additional properties "artifacts/payloads/api_product_ref_member_props.json" as "refMemberApiId"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "refMemberApiTwoId" and deployed it
    When I create an API product "${UNIQUE:RefProduct}" with context "${UNIQUE:refProductCtx}" from APIs "refMemberApiId,refMemberApiTwoId" as "refProductId"
    Then The response status code should be 201
    And The response array field "apis" should have exactly 2 entries
    When I put the following JSON payload in context as "refProductRev"
    """
    {"description":"ref-member product revision"}
    """
    And I make a request to create a revision for "api-products" resource "refProductId" with payload "refProductRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "refProductId"
    Then The response status code should be 201
    Then The publisher product list should report API product "refProductId" exactly once with the same info fields
    When I publish the "api-products" resource with id "refProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    When I retrieve the API product swagger of "refProductId"
    Then The response status code should be 200
    # The member's $ref'd operations are carried into the product definition.
    And The response should contain "/users/{userID}/products/{productID}"
    And I put the response payload in context as "refProductDefinition"
    When I validate the openapi definition captured as "refProductDefinition"
    Then The response status code should be 200
    And The value of response field "isValid" should be "true"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Legacy tested a context embedding a {version} template (its assertions sat in a catch block with no fail(),
  # so a successful create would have passed silently). Asserted unconditionally here, with the exact status and
  # error message the product returns.
  @cap:publisher @feat:products @type:negative @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product context embedding a version template is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "prodApiIdNeg" and deployed it
    When I attempt to create an API product "${UNIQUE:BadCtxProduct}" with context "${UNIQUE:badCtxProduct}{version}" from API "prodApiIdNeg"
    Then The response status code should be 400
    And The response should contain "The API Product context is malformed"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:negative @dep:publisher
  Scenario Outline: An API product context containing spaces is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "spaceCtxApiId" and deployed it
    When I attempt to create an API product "${UNIQUE:SpaceCtxProduct}" with context "invalid context with spaces" from API "spaceCtxApiId"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: Create a new version of an API product as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "verApiId" and deployed it
    When I create an API product "${UNIQUE:VerProduct}" with context "${UNIQUE:verProductCtx}" from API "verApiId" as "verProductId"
    Then The response status code should be 201
    When I create a new version "2.0.0" of API product "verProductId" with default version "false" as "verProductV2Id"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "verProductV2Id"
    Then The response status code should be 200
    And The response should contain "2.0.0"
    And The value of response field "version" should be "2.0.0"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A new API product version can be created as the default version as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "defVerApiId" and deployed it
    When I create an API product "${UNIQUE:DefVerProduct}" with context "${UNIQUE:defVerProductCtx}" from API "defVerApiId" as "defProductId"
    Then The response status code should be 201
    When I create a new version "2.0.0" of API product "defProductId" with default version "true" as "defProductV2Id"
    Then The response status code should be 201
    # Re-fetch the new version and confirm it is flagged as the default.
    And The "api-products" resource should reflect the updated "isDefaultVersion" as:
      """
      true
      """
    When I retrieve the "api-products" resource with id "defProductV2Id"
    Then The response status code should be 200
    And The value of response field "version" should be "2.0.0"

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Legacy copied a product that was already DEPLOYED and PUBLISHED (its two version tests differ only in the
  # default-version flag), so the copy is also taken from a published two-API product here.
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A new version of a published API product is created with default version <defaultVersion> as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pubVerApiId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "pubVerApiTwoId" and deployed it
    When I create an API product "${UNIQUE:PubVerProduct}" with context "${UNIQUE:pubVerProductCtx}" from APIs "pubVerApiId,pubVerApiTwoId" as "pubVerProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "pubVerRev"
    """
    {"description":"published-source product revision"}
    """
    And I make a request to create a revision for "api-products" resource "pubVerProductId" with payload "pubVerRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "pubVerProductId"
    Then The response status code should be 201
    Then The publisher product list should report API product "pubVerProductId" exactly once with the same info fields
    When I publish the "api-products" resource with id "pubVerProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    When I create a new version "2.0.0" of API product "pubVerProductId" with default version "<defaultVersion>" as "pubVerProductV2Id"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "pubVerProductV2Id"
    Then The response status code should be 200
    And The value of response field "version" should be "2.0.0"
    And The value of response field "isDefaultVersion" should be "<defaultVersion>"

    Examples:
      | actor                     | defaultVersion |
      | publisherUser             | false          |
      | publisherUser@tenant1.com | false          |
      | publisherUser             | true           |
      | publisherUser@tenant1.com | true           |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product tracks its underlying API after the API is updated as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "underApiId" and deployed it
    When I create an API product "${UNIQUE:TrackProduct}" with context "${UNIQUE:trackProductCtx}" from API "underApiId" as "trackProductId"
    Then The response status code should be 201
    # Update the underlying API (description), then confirm the product still references it + its operations.
    When I retrieve the "apis" resource with id "underApiId"
    And I put the response payload in context as "underApiPayload"
    When I update the "apis" resource "underApiId" and "underApiPayload" with configuration type "description" and value:
      """
      Updated backing API for product tracking
      """
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "trackProductId"
    Then The response status code should be 200
    And The response should contain "{{underApiId}}"
    And The response should contain "/customers/{id}"
    # Re-save the product after the underlying API changed (legacy PUT the fetched DTO straight back) and confirm
    # its entry for that API still mirrors the API's own resource set.
    And I put the response payload in context as "trackProductPayload"
    When I update "api-products" resource of id "trackProductId" with payload "trackProductPayload"
    Then The response status code should be 200
    And The API product "trackProductId" entry for API "underApiId" should have the same operations count as the API

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Legacy's product had a DEPLOYED revision when its underlying API changed and the product was re-saved.
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: A deployed API product tracks its underlying API after the API is updated as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "depTrackApiId" and deployed it
    When I create an API product "${UNIQUE:DepTrackProduct}" with context "${UNIQUE:depTrackProductCtx}" from API "depTrackApiId" as "depTrackProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "depTrackRev"
    """
    {"description":"deployed product revision before the member update"}
    """
    And I make a request to create a revision for "api-products" resource "depTrackProductId" with payload "depTrackRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "depTrackProductId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "depTrackApiId"
    And I put the response payload in context as "depTrackApiPayload"
    When I update the "apis" resource "depTrackApiId" and "depTrackApiPayload" with configuration type "description" and value:
      """
      Updated backing API for a deployed product
      """
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "depTrackProductId"
    Then The response status code should be 200
    And I put the response payload in context as "depTrackProductPayload"
    When I update "api-products" resource of id "depTrackProductId" with payload "depTrackProductPayload"
    Then The response status code should be 200
    And The response field "apis[*].apiId" should be exactly the list "{{depTrackApiId}}"
    And The API product "depTrackProductId" entry for API "depTrackApiId" should have the same operations count as the API

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # An APIM_ADMIN may create a product ON BEHALF OF another provider; the named provider is kept rather than
  # replaced by the caller (legacy created its revision product with an explicit provider as the super admin).
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: An admin creates an API product on behalf of another provider as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "onBehalfApiId" and deployed it
    When I create an API product "${UNIQUE:OnBehalfProduct}" with context "${UNIQUE:onBehalfProductCtx}" and provider of actor "<provider>" from APIs "onBehalfApiId" as "onBehalfProductId"
    Then The response status code should be 201
    And The create response of API product "onBehalfProductId" should echo its name, context, version and the provider of actor "<provider>"
    And The provider of "api-products" resource "onBehalfProductId" should match actor "<provider>"
    And The API product "onBehalfProductId" read from the publisher should match its create response
    Then The publisher product list should report API product "onBehalfProductId" exactly once with the same info fields
    When I put the following JSON payload in context as "onBehalfRev"
    """
    {"description":"on-behalf product revision"}
    """
    And I make a request to create a revision for "api-products" resource "onBehalfProductId" with payload "onBehalfRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "onBehalfProductId"
    Then The response status code should be 201
    And The provider of "api-products" resource "onBehalfProductId" should match actor "<provider>"

    Examples:
      | actor             | provider                  |
      | admin             | publisherUser             |
      | admin@tenant1.com | publisherUser@tenant1.com |

  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: API product revision lifecycle — create, list, deploy, undeploy, restore, delete as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "revApiId" and deployed it
    When I create an API product "${UNIQUE:RevProduct}" with context "${UNIQUE:revProductCtx}" from API "revApiId" as "revProductId"
    Then The response status code should be 201

    When I put the following JSON payload in context as "prodRevPayload"
    """
    {"description":"product revision 1"}
    """
    And I make a request to create a revision for "api-products" resource "revProductId" with payload "prodRevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "prodRevId"
    When I retrieve the revisions of "api-products" resource "revProductId"
    Then The response status code should be 200
    And The revision list should contain exactly revision "prodRevId"

    When I deploy revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    And I wait until "api-products" "revProductId" revision is deployed in the gateway
    # The deployed product revision actually reaches the gateway (legacy waitForDeployAPI read it back there).
    And the "api-products" resource "revProductId" should be live on the gateway, redeploying if propagation is lost and updating revision key "prodRevId"
    When I undeploy revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    When I restore revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 201
    # A restored product must still list its member APIs (the working copy is rebuilt from the revision).
    When I retrieve the "api-products" resource with id "revProductId"
    Then The response status code should be 200
    # The product has exactly one member API; the count stops a restore that dropped or duplicated it passing.
    And The response array field "apis[*].apiId" should have exactly 1 entries
    And The response field "apis[*].apiId" should be exactly the list "{{revApiId}}"
    When I delete revision "prodRevId" of "api-products" resource "revProductId"
    Then The response status code should be 200

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Legacy APIProductRevisionTestCase runs its revision lifecycle as an administrator against a product with two
  # member APIs and an arbitrary UUID provider. Keep that fixture distinct from the owner-created product above:
  # it protects admin authorization, multi-member revision snapshots, null-vhost undeploy, and restore fidelity.
  @cap:publisher @feat:revisions @type:regression @parity:api-product-revision-full-lifecycle @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: An admin manages revisions of a two-member API product with an arbitrary UUID provider as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "adminRevApiOneId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "adminRevApiTwoId" and deployed it
    When I create an API product "${UNIQUE:AdminRevisionProduct}" with context "${UNIQUE:adminRevisionProductCtx}" and provider "<provider>" from APIs "adminRevApiOneId,adminRevApiTwoId" as "adminRevProductId"
    Then The response status code should be 201
    And The create response of API product "adminRevProductId" should echo its name, context, version and requested provider for actor "<actor>"
    And The API product "adminRevProductId" read from the publisher should match its create response

    When I put the following JSON payload in context as "adminProdRevPayload"
    """
    {"description":"admin two-member product revision"}
    """
    And I make a request to create a revision for "api-products" resource "adminRevProductId" with payload "adminProdRevPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "adminProdRevId"
    When I retrieve the revisions of "api-products" resource "adminRevProductId"
    Then The response status code should be 200
    And The revision list should contain exactly revision "adminProdRevId"

    When I deploy revision "adminProdRevId" of "api-products" resource "adminRevProductId"
    Then The response status code should be 201
    And I wait until "api-products" "adminRevProductId" revision is deployed in the gateway
    And the "api-products" resource "adminRevProductId" should be live on the gateway, redeploying if propagation is lost and updating revision key "adminProdRevId"
    When I put the following JSON payload in context as "adminNullVhostUndeploy"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":null,"displayOnDevportal":true}]
    """
    And I undeploy revision "adminProdRevId" of "api-products" resource "adminRevProductId" with payload "adminNullVhostUndeploy"
    Then The response status code should be 201

    When I restore revision "adminProdRevId" of "api-products" resource "adminRevProductId"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "adminRevProductId"
    Then The response status code should be 200
    And The response array field "apis[*].apiId" should have exactly 2 entries
    And The response field "apis[*].apiId" should be exactly the list "{{adminRevApiOneId}},{{adminRevApiTwoId}}"
    When I delete revision "adminProdRevId" of "api-products" resource "adminRevProductId"
    Then The response status code should be 200

    Examples:
      | actor            | provider                                      |
      | admin             | 6b3e6f6c-c324-4ac2-a107-759677570919          |
      | admin@tenant1.com  | 901e00a1-9c62-4b13-8a9c-551d3ed4fc85@tenant1.com |

  @cap:publisher @feat:products @type:negative @dep:publisher @legacy:APIProductLifecycleTest
  Scenario Outline: A published API product with an active subscription cannot be deleted until it is retired as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "d5ApiId" and deployed it
    When I create an API product "${UNIQUE:DelSubProduct}" with context "${UNIQUE:delSubProductCtx}" from API "d5ApiId" as "d5ProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "d5Rev"
    """
    {"description":"product revision for delete-with-subscription"}
    """
    And I make a request to create a revision for "api-products" resource "d5ProductId" with payload "d5Rev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "d5ProductId"
    Then The response status code should be 201
    When I publish the "api-products" resource with id "d5ProductId"
    Then The response status code should be 200
    When I have set up application with keys, subscribed to API "d5ProductId", and obtained access token for "d5SubId"
    Then The response status code should be 200
    # Delete is rejected while an active subscription exists.
    When I delete the "api-products" resource with id "d5ProductId"
    Then The response status code should be 409
    And The response should contain "active subscriptions exist"
    # But the product can still be deprecated (the transition is auto-approved).
    When I change the lifecycle of "api-products" resource "d5ProductId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    When I retrieve the "api-products" resource with id "d5ProductId"
    Then The response should contain "DEPRECATED"
    # Retiring the product REMOVES its subscriptions, which is what then allows the delete to succeed.
    When I change the lifecycle of "api-products" resource "d5ProductId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    # The change-lifecycle response reports the state in title case ("Retired"), unlike the product DTO's
    # uppercase "state" field — both are asserted verbatim, each in its own representation.
    And The value of response field "lifecycleState.state" should be "Retired"
    When I retrieve the subscriptions of API "d5ProductId"
    Then The response status code should be 200
    And The subscription list should contain exactly 0 subscriptions
    When I delete the "api-products" resource with id "d5ProductId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:revisions @type:regression @dep:publisher @legacy:APIProductRevisionTestCase
  Scenario Outline: Restoring the underlying API to a revision missing resources a product depends on is rejected as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "d6ApiId" and deployed it
    # Revision 1 captures the API's original resources (/customers/{id}).
    When I put the following JSON payload in context as "d6Rev1Payload"
    """
    {"description":"revision 1 - original resources"}
    """
    And I make a request to create a revision for "apis" resource "d6ApiId" with payload "d6Rev1Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "d6Rev1Id"
    # Add new resources to the API, then capture revision 2 with the enlarged resource set.
    When I retrieve the "apis" resource with id "d6ApiId"
    And I put the response payload in context as "d6ApiPayload"
    When I update the "apis" resource "d6ApiId" and "d6ApiPayload" with configuration type "operations" and value:
      """
      [{"target":"/customers/{id}","verb":"GET"},{"target":"/customers/{id}","verb":"DELETE"},{"target":"/missing-resource","verb":"GET"},{"target":"/missing-resource","verb":"POST"},{"target":"/missing-resource/{id}","verb":"GET"}]
      """
    Then The response status code should be 200
    When I put the following JSON payload in context as "d6Rev2Payload"
    """
    {"description":"revision 2 - with added resources"}
    """
    And I make a request to create a revision for "apis" resource "d6ApiId" with payload "d6Rev2Payload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "d6Rev2Id"
    # A product aggregates the API's current (enlarged) resource set, then is deployed.
    When I create an API product "${UNIQUE:RestoreProduct}" with context "${UNIQUE:restoreProductCtx}" from API "d6ApiId" as "d6ProductId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "d6ProdRev"
    """
    {"description":"product revision"}
    """
    And I make a request to create a revision for "api-products" resource "d6ProductId" with payload "d6ProdRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "d6ProductId"
    Then The response status code should be 201
    # Restoring the API to revision 2 (which has the product's resources) succeeds.
    When I restore revision "d6Rev2Id" of "apis" resource "d6ApiId"
    Then The response status code should be 201
    # Restoring to revision 1 (which lacks the added resources the product depends on) is rejected.
    When I restore revision "d6Rev1Id" of "apis" resource "d6ApiId"
    Then The response status code should be 400

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Acts as admin rather than publisherUser because the arc also reads the product from the DEVPORTAL (the
  # publisher-only role carries no consumer scopes), which is where the published product's DTO fidelity lives.
  @cap:publisher @feat:products @type:regression @dep:publisher @legacy:APIProductLifecycleTest
  Scenario Outline: An API product moves through its lifecycle and can be deleted when retired as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "lcApiId" and deployed it
    When I create an API product "${UNIQUE:LcProduct}" with context "${UNIQUE:lcProductCtx}" from API "lcApiId" as "lcProductId"
    Then The response status code should be 201
    # Publish → Deprecate → Retire, confirming each transition on the publisher plane.
    When I publish the "api-products" resource with id "lcProductId"
    Then The response status code should be 200
    # The Publish transition is auto-approved, lands the product in Published and offers exactly four onward
    # transitions (legacy asserted the count, not merely their presence) — which four is pinned as well.
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    And The response array field "lifecycleState.availableTransitions" should have exactly 4 entries
    And The response field "lifecycleState.availableTransitions[*].event" should be exactly the list "Block,Deploy as a Prototype,Demote to Created,Deprecate"
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "PUBLISHED"
    # A published product is visible in the devportal and its representation agrees with the publisher DTO.
    Then The devportal should report API product "lcProductId" exactly once with the same fields
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Deprecate"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "DEPRECATED"
    When I change the lifecycle of "api-products" resource "lcProductId" with action "Retire"
    Then The response status code should be 200
    When I retrieve the "api-products" resource with id "lcProductId"
    Then The response should contain "RETIRED"
    # A retired product can be deleted (legacy testDeleteRetiredAPIProducts).
    When I delete the "api-products" resource with id "lcProductId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # The rest of legacy's SUPER_TENANT_USER_STORE_USER fan-out — testCreateAPIProduct (internal key while CREATED),
  # testPublishAPIProduct, testChangeAPIProductLifecycleStateToBlockedState, testDeleteDeprecatedAPIProductsWithSubscription
  # and testDeleteRetiredAPIProducts — driven end to end by SECONDARY.COM store actors, on the publisher, devportal
  # AND gateway planes. It is a separate scenario rather than extra Examples rows on the admin arcs because there is
  # NO admin store actor to fan out to: the primary admin role poisons a store account, which then authenticates as
  # 401. Two store principals cover the same ground instead — the store publisher drives the publisher plane and the
  # store subscriber the consumer plane (its OWN password-grant token is what the gateway validates), both inside
  # the same tenant. This feature's block starts the node backend for the gateway half.
  @cap:publisher @feat:products @rule:secondary-userstore @type:regression @dep:publisher @dep:devportal @dep:admin @dep:gateway @legacy:APIProductLifecycleTest
  Scenario Outline: An API product created by a secondary-store publisher completes its lifecycle and can be deleted when retired as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_product_lifecycle_api.json" as "storeProdApiId" and deployed it
    And the "apis" resource "storeProdApiId" should be live on the gateway, redeploying if propagation is lost
    And I have created an api from "artifacts/payloads/create_apim_product_leasing_api.json" as "storeProdApiTwoId" and deployed it
    And the "apis" resource "storeProdApiTwoId" should be live on the gateway, redeploying if propagation is lost
    When I create an API product "${UNIQUE:StoreProduct}" with context "${UNIQUE:storeProductCtx}" from APIs "storeProdApiId,storeProdApiTwoId" as "storeProductId"
    Then The response status code should be 201
    And The value of response field "state" should be "CREATED"
    And The create response of API product "storeProductId" should echo its name, context, version and the provider of actor "<actor>"
    When I put the following JSON payload in context as "storeProdRev"
    """
    {"description":"product revision for the secondary-store lifecycle"}
    """
    And I make a request to create a revision for "api-products" resource "storeProductId" with payload "storeProdRev"
    Then The response status code should be 201
    When I deploy revision "revisionId" of "api-products" resource "storeProductId"
    Then The response status code should be 201
    And the "api-products" resource "storeProductId" should be live on the gateway, redeploying if propagation is lost
    And The API product "storeProductId" read from the publisher should match its create response
    When I retrieve the "api-products" resource with id "storeProductId"
    And I extract response field "context" and store it as "storeProductContext"
    # CREATED but deployed: the store publisher's internal key invokes it, on the versioned and versionless routes.
    When I generate an internal API key for API "storeProductId" and store it as "storeInternalKey"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using internal key "storeInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123/" with method "GET" using internal key "storeInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "\"name\":\"John\""
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" using internal key "storeInternalKey" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Publish: auto-approved, lands in Published and offers exactly the four onward transitions.
    When I publish the "api-products" resource with id "storeProductId"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    And The response array field "lifecycleState.availableTransitions" should have exactly 4 entries
    # WHICH four, not just how many — a count alone is satisfied by any four transitions.
    And The response field "lifecycleState.availableTransitions[*].event" should be exactly the list "Block,Deploy as a Prototype,Demote to Created,Deprecate"
    # A store SUBSCRIBER in the same tenant sees the published product, subscribes, keys an application and
    # invokes with its OWN password-grant token.
    Given The system is ready and I have valid devportal access token as "SECONDARY.COM/subscriberUser1<tenantSuffix>"
    Then The devportal should report API product "storeProductId" with lifecycle status "PUBLISHED"
    And The devportal should report API product "storeProductId" exactly once with the same fields as published by "<actor>"
    And The devportal should advertise gateway endpoint URLs for API product "storeProductId"
    # The store subscriber subscribes an application of its own (subscription and key DTOs asserted) and takes
    # application tokens plus its OWN password-grant user tokens on both key mappings.
    When I have set up application with production and sandbox keys, subscribed to API "storeProductId" with plan "Unlimited", and obtained the four credentials as "storeProdSubId"
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "sandboxUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/assets" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The response should contain "Hello World"
    When I invoke every operation of the product leasing member at "{{storeProductContext}}/1.0.0" with access tokens "productionUserToken,productionAppToken" expecting status 200 and body "Hello World"
    # Block, then Re-Publish — the blocked product refuses the store subscriber (503) and serves it again after.
    Given I act as "<actor>"
    When I change the lifecycle of "api-products" resource "storeProductId" with action "Block"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Blocked"
    And The response field "lifecycleState.availableTransitions[*].event" should be exactly the list "Deprecate,Re-Publish"
    Given I act as "SECONDARY.COM/subscriberUser1<tenantSuffix>"
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" without authentication until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123/" with method "GET" without authentication until response status code becomes 503 within 60 seconds
    Then The response status code should be 503
    Given I act as "<actor>"
    When I change the lifecycle of "api-products" resource "storeProductId" with action "Re-Publish"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Published"
    Given I act as "SECONDARY.COM/subscriberUser1<tenantSuffix>"
    Then The devportal should report API product "storeProductId" with lifecycle status "PUBLISHED"
    And The devportal should report API product "storeProductId" exactly once with the same fields as published by "<actor>"
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke every operation of the product leasing member at "{{storeProductContext}}/1.0.0" with access tokens "productionUserToken,sandboxUserToken" expecting status 200 and body "Hello World"
    # Back as the store publisher: delete is refused while that subscription is active.
    Given I act as "<actor>"
    When I delete the "api-products" resource with id "storeProductId"
    Then The response status code should be 409
    And The response should contain "active subscriptions exist"
    When I change the lifecycle of "api-products" resource "storeProductId" with action "Deprecate"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    When I retrieve the "api-products" resource with id "storeProductId"
    Then The response should contain "DEPRECATED"
    And I wait until API product "storeProductId" has lifecycle state "Deprecated" and transition "Retire" available
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers" with method "POST" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "PUT" using access token "productionUserToken" and payload "" with content type "text/plain" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{storeProductContext}}/customers/123" with method "DELETE" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # A deprecated product is still readable on the devportal and still serves its subscriber.
    Given I act as "SECONDARY.COM/subscriberUser1<tenantSuffix>"
    Then The devportal should report API product "storeProductId" with lifecycle status "DEPRECATED"
    When I invoke the API at gateway context "{{storeProductContext}}/1.0.0/customers/123/" with method "GET" using access token "productionUserToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "name" should be "John"
    When I invoke every operation of the product leasing member at "{{storeProductContext}}/1.0.0" with access tokens "productionUserToken" expecting status 200 and body "Hello World"
    # Retiring removes the subscription, which is what then allows the delete to succeed.
    Given I act as "<actor>"
    When I change the lifecycle of "api-products" resource "storeProductId" with action "Retire"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "APPROVED"
    And The value of response field "lifecycleState.state" should be "Retired"
    When I retrieve the subscriptions of API "storeProductId"
    Then The response status code should be 200
    And The subscription list should contain exactly 0 subscriptions
    When I delete the "api-products" resource with id "storeProductId"
    Then The response status code should be 200

    Examples:
      | actor                                    | tenantSuffix |
      | SECONDARY.COM/publisherUser1             |              |
      | SECONDARY.COM/publisherUser1@tenant1.com | @tenant1.com |

  # Mutual-SSL security on an API PRODUCT: the product (over two APIs) is switched to mutualssl +
  # mutualssl_mandatory, a client certificate is uploaded to the PRODUCT (products have no separate
  # client-certificates path, so the apis path is addressed with the product's uuid — exactly what the legacy
  # client did), and the product then still revisions + deploys. Ports
  # APIProductCreationTestCase#testCreateAndDeployApiProductWithMutualSSLEnabled, which asserted only a non-null
  # revision uuid; the securityScheme echo and the certificate listing are asserted here in addition. No gateway
  # invocation: an mTLS handshake needs the SSL-profile overlay of gateway/mutual_ssl_invocation's block.
  @cap:publisher @feat:products @rule:mutual-ssl @type:regression @dep:publisher @legacy:APIProductCreationTestCase
  Scenario Outline: An API product can carry mutual-SSL security and a client certificate as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "mtlsProdApiOneId" and deployed it
    And I have created an api from "artifacts/payloads/create_apim_test_api_two.json" as "mtlsProdApiTwoId" and deployed it
    When I create an API product "${UNIQUE:MtlsProduct}" with context "${UNIQUE:mtlsProductCtx}" from APIs "mtlsProdApiOneId,mtlsProdApiTwoId" as "mtlsProductId"
    Then The response status code should be 201
    When I retrieve the "api-products" resource with id "mtlsProductId"
    And I put the response payload in context as "mtlsProductPayload"
    When I update the "api-products" resource "mtlsProductId" and "mtlsProductPayload" with configuration type "securityScheme" and value:
      """
      ["mutualssl","mutualssl_mandatory"]
      """
    Then The response status code should be 200
    And The response field "securityScheme" should be exactly the list "mutualssl,mutualssl_mandatory"
    # Upload the accepted client certificate to the PRODUCT and confirm it is attached to it.
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "<certAlias>" to API "mtlsProductId" for tier "Unlimited"
    Then The response status code should be 201
    And The client certificates of API product "mtlsProductId" should list alias "<certAlias>"
    # Legacy uploaded to the key-type-scoped endpoint with key type SANDBOX.
    When I upload client certificate "artifacts/certs/mutualssl/cert_chain_intermediate.cer" with alias "<certAlias>sb" and key type "SANDBOX" to API "mtlsProductId" for tier "Unlimited"
    Then The response status code should be 201
    And The "SANDBOX" client certificates of API product "mtlsProductId" should list alias "<certAlias>sb"
    # A mutual-SSL product still revisions and deploys.
    When I put the following JSON payload in context as "mtlsProductRev"
    """
    {"description":"mutual-ssl product revision"}
    """
    And I make a request to create a revision for "api-products" resource "mtlsProductId" with payload "mtlsProductRev"
    Then The response status code should be 201
    And I extract response field "id" and store it as "mtlsProductRevId"
    When I deploy revision "mtlsProductRevId" of "api-products" resource "mtlsProductId"
    Then The response status code should be 201

    Examples:
      | actor                     | certAlias         |
      | publisherUser             | mtlsproductsuper  |
      | publisherUser@tenant1.com | mtlsproducttenant |
