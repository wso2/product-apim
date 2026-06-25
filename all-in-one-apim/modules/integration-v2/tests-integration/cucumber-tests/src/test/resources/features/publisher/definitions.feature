@cleanup
Feature: Publisher API Definition Import

  Publisher-plane import of OpenAPI definitions (OAS 2, 3 and 3.1): import the definition to create an
  API, then take it through revision, deploy and publish. Asserts only publisher outcomes — invoking the
  imported API at the gateway is covered by gateway/rest-invocation. Each row is self-contained and torn
  down by the per-scenario cleanup hook.

  @cap:publisher @feat:definitions @type:regression @legacy:ImportOpenApiDefinitionTestCase
  Scenario Outline: Import an OpenAPI definition and publish it as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I import open api definition from "<apiDefinition>" , additional properties from "<additionalProperty>" and create api as "importedApiId"
    Then The response status code should be 201

    When I retrieve the "apis" resource with id "importedApiId"
    Then The response status code should be 200
    And I put the response payload in context as "importedApiPayload"

    When I put the following JSON payload in context as "createRevisionPayload"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "importedApiId" with payload "createRevisionPayload"
    Then The response status code should be 201

    When I deploy revision "revisionId" of "apis" resource "importedApiId"
    Then The response status code should be 201
    And I wait for deployment of the resource in "importedApiPayload"

    When I publish the "apis" resource with id "importedApiId"
    Then The response status code should be 200
    And The lifecycle status of API "importedApiId" should be "Published"

    Examples:
      | actor                     | apiDefinition                                   | additionalProperty                                     |
      | publisherUser             | artifacts/payloads/OAS/OAS2ApiDefinition.json   | artifacts/payloads/OAS/OAS2AdditionalProperties.json   |
      | publisherUser             | artifacts/payloads/OAS/OAS3ApiDefinition.json   | artifacts/payloads/OAS/OAS3AdditionalProperties.json   |
      | publisherUser             | artifacts/payloads/OAS/OAS3.1ApiDefinition.json | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS2ApiDefinition.json   | artifacts/payloads/OAS/OAS2AdditionalProperties.json   |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS3ApiDefinition.json   | artifacts/payloads/OAS/OAS3AdditionalProperties.json   |
      | publisherUser@tenant1.com | artifacts/payloads/OAS/OAS3.1ApiDefinition.json | artifacts/payloads/OAS/OAS3.1AdditionalProperties.json |

  @cap:publisher @feat:definitions @type:negative @legacy:ImportOpenApiDefinitionTestCase
  Scenario Outline: A subscriber-role user cannot create an API to import a definition into as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "subscriberApiPayload"
    And I attempt to create an "apis" resource with payload "subscriberApiPayload"
    Then The response status code should be 401

    Examples:
      | actor                       |
      | subscriberUser              |
      | subscriberUser@tenant1.com  |
