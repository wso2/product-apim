@cleanup
Feature: Publisher API creation

  Creation-time API security and revision deployment coverage ported from APICreationTestCase. The mTLS scenario
  runs as the least-privileged publisher in both the super-tenant and tenant1.com contexts. Revision creation waits
  for the publisher API to become readable, asserts the exact 201 response, extracts a non-empty revision ID,
  and deploys that same ID; the bounded deployment-status poll replaces any fixed delay.

  AI API configuration: an AIAPI-subtype API references its LLM provider by name and API version, by id, or by
  both, and the product resolves name and version first with the id as the fallback. A reference that resolves to
  no provider of the API's own organization must be rejected with 400 / 903105 BEFORE anything is persisted. Each
  rejection is followed by a create under the same name and context, which a partially created API would turn
  into a 409. The resolving references run over several built-in providers in both tenants, as the tenant admin
  because a provider's id is read from the admin-plane provider listing; the rejections need no id and run as the
  least-privileged publisher. These scenarios are management-plane only (no deploy, publish or invocation).

  @cap:publisher @feat:api-lifecycle @rule:api-creation @type:regression @legacy:APICreationTestCase
  Scenario Outline: Create and deploy an mTLS API with a non-empty revision ID as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put JSON payload from file "artifacts/payloads/create_apim_mutualssl_api.json" in context as "creationMtlsPayload"
    And I create an "apis" resource with payload "creationMtlsPayload" as "creationMtlsApiId"
    Then The response status code should be 201
    When I generate a unique alphanumeric value and store it as "creationMtlsCertAlias"
    And I upload client certificate "artifacts/certs/mutualssl/cert_chain_root.cer" with alias "{{creationMtlsCertAlias}}" and key type "SANDBOX" to API "creationMtlsApiId" for tier "Unlimited"
    Then The response status code should be 201
    When I put the following JSON payload in context as "creationMtlsRevisionPayload"
    """
    {"description":"mTLS API creation revision"}
    """
    And I make a request to create a revision for "apis" resource "creationMtlsApiId" with payload "creationMtlsRevisionPayload"
    Then The response status code should be 201
    And I extract response field "id" and store it as "creationMtlsRevisionId"
    When I deploy revision "creationMtlsRevisionId" of "apis" resource "creationMtlsApiId"
    Then The response status code should be 201
    And I wait until "apis" "creationMtlsApiId" revision is deployed in the gateway

    Examples:
      | actor                     |
      | publisherUser             |
      | publisherUser@tenant1.com |

  # Every reference form that resolves must bind the API to the provider of the API's own organization: the stored
  # id is what proves which provider a reference resolved to, so each create is read back and its binding pinned.
  @cap:publisher @feat:api-lifecycle @rule:ai-api-configuration @type:regression @dep:admin
  Scenario Outline: Every resolvable reference to <provider> <apiVersion> binds the AI API to the local provider as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the AI service providers
    Then The AI service provider "<provider>" version "<apiVersion>" should be listed with deprecated "false" and stored as "aiCfgLocalProviderId"
    When I put literal value "<provider>" in context as "aiCfgProviderName"
    And I put literal value "<apiVersion>" in context as "aiCfgProviderApiVersion"
    # By name and API version
    And I put a unique value from base "aiCfgApi" in context as "aiCfgApiName"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_name_version.json" in context as "aiCfgPayload"
    And I create an "apis" resource with payload "aiCfgPayload" as "aiCfgNameVersionApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "aiCfgNameVersionApiId"
    Then The response status code should be 200
    And The AI API in the response should be bound to AI service provider "<provider>" version "<apiVersion>" with id "aiCfgLocalProviderId"
    # By id only: the reference the Publisher UI's Create AI API flow sends
    When I put value "aiCfgLocalProviderId" in context as "aiCfgProviderId"
    And I put a unique value from base "aiCfgApi" in context as "aiCfgApiName"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_id.json" in context as "aiCfgPayload"
    And I create an "apis" resource with payload "aiCfgPayload" as "aiCfgIdApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "aiCfgIdApiId"
    Then The response status code should be 200
    And The AI API in the response should be bound to AI service provider "<provider>" version "<apiVersion>" with id "aiCfgLocalProviderId"
    # By name and API version plus another environment's id, as an exported api.yaml carries it: an id that
    # differs from the local one must neither be rejected nor stored
    When I generate a random UUID and store it as "aiCfgProviderId"
    And I put a unique value from base "aiCfgApi" in context as "aiCfgApiName"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_name_version_id.json" in context as "aiCfgPayload"
    And I create an "apis" resource with payload "aiCfgPayload" as "aiCfgForeignIdApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "aiCfgForeignIdApiId"
    Then The response status code should be 200
    And The AI API in the response should be bound to AI service provider "<provider>" version "<apiVersion>" with id "aiCfgLocalProviderId"

    Examples:
      | provider   | apiVersion | actor             |
      | OpenAI     | 2.0.0      | admin             |
      | AWSBedrock | 1.0.0      | admin             |
      | Anthropic  | 1.0.0      | admin             |
      | Gemini     | 1.1.0      | admin             |
      | OpenAI     | 2.0.0      | admin@tenant1.com |
      | AWSBedrock | 1.0.0      | admin@tenant1.com |
      | Anthropic  | 1.0.0      | admin@tenant1.com |
      | Gemini     | 1.1.0      | admin@tenant1.com |

  # Unused <name>/<apiVersion> cells are "-": the row's payload file does not reference that placeholder.
  @cap:publisher @feat:api-lifecycle @rule:ai-api-configuration @type:negative @type:regression
  Scenario Outline: An AI API with <case> is rejected without being created as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I put a unique value from base "aiCfgApi" in context as "aiCfgApiName"
    And I put literal value "<name>" in context as "aiCfgProviderName"
    And I put literal value "<apiVersion>" in context as "aiCfgProviderApiVersion"
    And I generate a random UUID and store it as "aiCfgProviderId"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_<config>.json" in context as "aiCfgPayload"
    And I attempt to create an "apis" resource with payload "aiCfgPayload"
    Then The response status code should be 400
    And The value of error response field "code" should be "903105"
    And The value of error response field "description" should be "Invalid AI configuration. <reason>"
    # Nothing was persisted: the same name and context are still free for an API whose provider resolves
    When I put literal value "OpenAI" in context as "aiCfgProviderName"
    And I put literal value "2.0.0" in context as "aiCfgProviderApiVersion"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_name_version.json" in context as "aiCfgPayload"
    And I create an "apis" resource with payload "aiCfgPayload" as "aiCfgApiId"
    Then The response status code should be 201

    Examples: super tenant
      | case                                          | config          | name              | apiVersion | reason                                                                                                                                  | actor         |
      | an unknown provider name                      | name_version    | NoSuchLLMProvider | 1.0.0      | LLM provider with name 'NoSuchLLMProvider' and API version '1.0.0' not found.                                                           | publisherUser |
      | an unknown API version of a built-in provider | name_version    | AWSBedrock        | 9.9.9      | LLM provider with name 'AWSBedrock' and API version '9.9.9' not found.                                                                  | publisherUser |
      | a provider name without an API version        | name_only       | Anthropic         | -          | Both llmProviderName and llmProviderApiVersion must be provided.                                                                        | publisherUser |
      | an API version without a provider name        | version_only    | -                 | 1.1.0      | Both llmProviderName and llmProviderApiVersion must be provided.                                                                        | publisherUser |
      | an unknown provider id                        | id              | -                 | -          | LLM provider with ID '{{aiCfgProviderId}}' not found.                                                                                   | publisherUser |
      | an unknown provider name, version and id      | name_version_id | NoSuchLLMProvider | 1.0.0      | LLM provider with name 'NoSuchLLMProvider' and API version '1.0.0' not found, and LLM provider with ID '{{aiCfgProviderId}}' not found. | publisherUser |
      | an empty AI configuration                     | empty           | -                 | -          | Either llmProviderName and llmProviderApiVersion, or llmProviderId must be provided.                                                    | publisherUser |
      | no AI configuration                           | no_config       | -                 | -          | LLM provider details are missing.                                                                                                       | publisherUser |

    Examples: tenant1.com
      | case                                          | config          | name              | apiVersion | reason                                                                                                                                  | actor                     |
      | an unknown provider name                      | name_version    | NoSuchLLMProvider | 1.0.0      | LLM provider with name 'NoSuchLLMProvider' and API version '1.0.0' not found.                                                           | publisherUser@tenant1.com |
      | an unknown API version of a built-in provider | name_version    | Gemini            | 9.9.9      | LLM provider with name 'Gemini' and API version '9.9.9' not found.                                                                      | publisherUser@tenant1.com |
      | a provider name without an API version        | name_only       | OpenAI            | -          | Both llmProviderName and llmProviderApiVersion must be provided.                                                                        | publisherUser@tenant1.com |
      | an API version without a provider name        | version_only    | -                 | 1.0.0      | Both llmProviderName and llmProviderApiVersion must be provided.                                                                        | publisherUser@tenant1.com |
      | an unknown provider id                        | id              | -                 | -          | LLM provider with ID '{{aiCfgProviderId}}' not found.                                                                                   | publisherUser@tenant1.com |
      | an unknown provider name, version and id      | name_version_id | NoSuchLLMProvider | 1.0.0      | LLM provider with name 'NoSuchLLMProvider' and API version '1.0.0' not found, and LLM provider with ID '{{aiCfgProviderId}}' not found. | publisherUser@tenant1.com |
      | an empty AI configuration                     | empty           | -                 | -          | Either llmProviderName and llmProviderApiVersion, or llmProviderId must be provided.                                                    | publisherUser@tenant1.com |
      | no AI configuration                           | no_config       | -                 | -          | LLM provider details are missing.                                                                                                       | publisherUser@tenant1.com |

  # Built-in providers are registered per organization with their own ids, so the super tenant's id names no
  # provider in tenant1.com. Spans both tenants in one scenario, so each actor is set explicitly.
  @cap:publisher @feat:api-lifecycle @rule:ai-api-configuration @type:negative @dep:admin
  Scenario Outline: An AI API in tenant1.com cannot reference the super tenant's <provider> <apiVersion> provider by id
    Given The system is ready
    And I have valid access tokens as "admin"
    When I retrieve the AI service providers
    Then The AI service provider "<provider>" version "<apiVersion>" should be listed with deprecated "false" and stored as "aiCfgProviderId"
    When I have valid access tokens as "admin@tenant1.com"
    And I put a unique value from base "aiCfgApi" in context as "aiCfgApiName"
    And I put JSON payload from file "artifacts/payloads/ai/ai_api_config_id.json" in context as "aiCfgPayload"
    And I attempt to create an "apis" resource with payload "aiCfgPayload"
    Then The response status code should be 400
    And The value of error response field "code" should be "903105"
    And The value of error response field "description" should be "Invalid AI configuration. LLM provider with ID '{{aiCfgProviderId}}' not found."
    # The tenant's own provider of the same name and version resolves by id, under the same name and context
    When I retrieve the AI service providers
    Then The AI service provider "<provider>" version "<apiVersion>" should be listed with deprecated "false" and stored as "aiCfgProviderId"
    When I put JSON payload from file "artifacts/payloads/ai/ai_api_config_id.json" in context as "aiCfgPayload"
    And I create an "apis" resource with payload "aiCfgPayload" as "aiCfgApiId"
    Then The response status code should be 201

    Examples:
      | provider   | apiVersion |
      | OpenAI     | 2.0.0      |
      | AWSBedrock | 1.0.0      |
      | Anthropic  | 1.0.0      |
      | Gemini     | 1.1.0      |
