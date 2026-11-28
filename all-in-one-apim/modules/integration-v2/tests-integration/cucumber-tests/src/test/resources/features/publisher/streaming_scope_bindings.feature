@cleanup
Feature: Publisher Streaming API Scope Role Bindings

  Scope role bindings of streaming (AsyncAPI) APIs created or updated from a definition (wso2/api-manager#5301).
  Before the fix, the definition was stored as supplied: without x-scopes-bindings the API's scopes showed no roles,
  even for a shared scope bound to a role or a local scope whose role was declared in api.yaml, and local scopes were
  registered in the key manager without roles. Each flow (import-asyncapi, project import, PUT asyncapi, service
  catalog import and reimport) must now write the resolved roles into the stored definition and register local
  scopes with them. Roles are deliberately NOT validated, so bad input is stored as given, exactly as before.

  Every scope, API name and context is scenario-unique (scope keys are tenant-wide). Three views are asserted: the
  stored definition's x-scopes-bindings, the API's scopes and, for local scopes, the scope as registered in the
  key manager (what the token endpoint enforces). Self-contained scenarios, torn down by the per-scenario cleanup
  hook. Runs as a tenant admin in both tenants, since shared scopes and key manager reads need admin rights.

  Background:
    Given The system is ready
    And I put a unique value from base "SseScopeApi" in context as "sbApiName"
    And I put a unique value from base "ssescope" in context as "sbContext"
    And I put a unique value from base "sbLocal" in context as "sbScope"

  # ---- Shared scopes: roles always come from the key manager -------------------------------------------------

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: import-asyncapi writes a shared scope's key manager role into the definition as <actor>
    Given I have valid access tokens as "<actor>"
    And I create a new shared scope as "sbSharedScope" bound to role "admin"
    Then The response status code should be 201
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/shared_unbound.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The response status code should be 200
    And The AsyncAPI definition in the response should bind scope "{{sbSharedScope}}" to role "admin"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbSharedScope}}')].scope.bindings[*]" should be exactly the list "admin"
    And The response array field "scopes[?(@.scope.name=='{{sbSharedScope}}')]" should have exactly 1 entries

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A project import writes a shared scope's key manager role into the definition as <actor>
    Given I have valid access tokens as "<actor>"
    And I create a new shared scope as "sbSharedScope" bound to role "admin"
    Then The response status code should be 201
    When I build an API project archive from directory "artifacts/payloads/async/scope-bindings/project_shared_unbound" as "sbArchive"
    And I import the API project archive "sbArchive" with overwrite "false"
    Then The response status code should be 200
    When I find the Publisher API named "{{sbApiName}}" and store its id as "sbApiId"
    And I retrieve the AsyncAPI definition of API "sbApiId"
    Then The response status code should be 200
    And The AsyncAPI definition in the response should bind scope "{{sbSharedScope}}" to role "admin"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbSharedScope}}')].scope.bindings[*]" should be exactly the list "admin"
    And The response array field "scopes[?(@.scope.name=='{{sbSharedScope}}')]" should have exactly 1 entries

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: Replacing the definition with PUT asyncapi keeps a shared scope's key manager role as <actor>
    Given I have valid access tokens as "<actor>"
    And I create a new shared scope as "sbSharedScope" bound to role "admin"
    Then The response status code should be 201
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/shared_unbound.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I update the AsyncAPI definition of API "sbApiId" from file "artifacts/payloads/async/scope-bindings/shared_unbound.yaml"
    Then The response status code should be 200
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbSharedScope}}" to role "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ---- Local scopes: roles from the definition or api.yaml, registered in the key manager --------------------

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: import-asyncapi registers a local scope bound in the definition with its role as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_bound_admin.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "admin"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list "admin"
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A role-less scope is left OUT of x-scopes-bindings (not written as ""), so an exported project can take its role
  # from api.yaml on re-import.
  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A local scope without a role gets no binding entry as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_unbound.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should have no binding for scope "{{sbScope}}"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list ""
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A project import applies a local scope's role declared only in api.yaml as <actor>
    Given I have valid access tokens as "<actor>"
    When I build an API project archive from directory "artifacts/payloads/async/scope-bindings/project_apiyaml_role" as "sbArchive"
    And I import the API project archive "sbArchive" with overwrite "false"
    Then The response status code should be 200
    When I find the Publisher API named "{{sbApiName}}" and store its id as "sbApiId"
    And I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "admin"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list "admin"
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A binding written in the definition wins over the api.yaml role on project import as <actor>
    Given I have valid access tokens as "<actor>"
    When I build an API project archive from directory "artifacts/payloads/async/scope-bindings/project_explicit_binding" as "sbArchive"
    And I import the API project archive "sbArchive" with overwrite "false"
    Then The response status code should be 200
    When I find the Publisher API named "{{sbApiName}}" and store its id as "sbApiId"
    And I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "Internal/creator"
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list "Internal/creator"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An explicit "" binding in the definition also wins over api.yaml at import time: no role is applied.
  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: An explicit empty binding in the definition wins over the api.yaml role as <actor>
    Given I have valid access tokens as "<actor>"
    When I build an API project archive from directory "artifacts/payloads/async/scope-bindings/project_explicit_empty_binding" as "sbArchive"
    And I import the API project archive "sbArchive" with overwrite "false"
    Then The response status code should be 200
    When I find the Publisher API named "{{sbApiName}}" and store its id as "sbApiId"
    And I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list ""
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PUT takes roles only from the uploaded file (as for REST): removing the binding removes the role everywhere,
  # with no fallback to the API's existing roles.
  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: Removing a local binding with PUT asyncapi removes the role as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_bound_admin.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response field "bindings" should be exactly the list "admin"
    When I update the AsyncAPI definition of API "sbApiId" from file "artifacts/payloads/async/scope-bindings/local_unbound.yaml"
    Then The response status code should be 200
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should have no binding for scope "{{sbScope}}"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list ""
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list ""

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Export round trip: the exported definition carries no binding for a role-less scope, so a role added to the
  # exported api.json is applied when the project is imported back over the API.
  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A role added to an exported project's api.json is applied on re-import as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_unbound.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I export the API "sbApiId" to an archive as "sbExport"
    Then The entry "Definitions/asyncapi" of archive "sbExport" should not contain "x-scopes-bindings"
    When I edit the entry "/api." of archive "sbExport" replacing pattern ".bindings.\s*:\s*\[\s*\]" with '"bindings": ["admin"]'
    And I import the API project archive "sbExport" with overwrite "true"
    Then The response status code should be 200
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "admin"
    When I retrieve the "apis" resource with id "sbApiId"
    Then The response field "scopes[?(@.scope.name=='{{sbScope}}')].scope.bindings[*]" should be exactly the list "admin"
    And The response array field "scopes[?(@.scope.name=='{{sbScope}}')]" should have exactly 1 entries
    When I retrieve the key manager scope "{{sbScope}}"
    Then The response status code should be 200
    And The response field "bindings" should be exactly the list "admin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ---- Service catalog: import and reimport --------------------------------------------------------------------

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: Service catalog import and reimport write the definition's roles as <actor>
    Given I have valid access tokens as "<actor>"
    And I put a unique value from base "sbService" in context as "sbServiceName"
    When I create an AsyncAPI service catalog entry named "{{sbServiceName}}" version "1.0.0" key "{{sbServiceName}}-key" from definition "artifacts/payloads/async/scope-bindings/service_local_bound_admin.yaml" as "sbServiceId"
    And I create an API from service catalog key "{{sbServiceName}}-key" named "{{sbApiName}}" with context "/{{sbContext}}" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "admin"
    When I update the AsyncAPI service catalog entry "sbServiceId" named "{{sbServiceName}}" version "1.0.0" key "{{sbServiceName}}-key" from definition "artifacts/payloads/async/scope-bindings/service_local_bound_badrole.yaml"
    Then The response status code should be 200
    When I reimport the service catalog definition of API "sbApiId"
    Then The response status code should be 200
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "nosuchrole"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # ---- Roles are not validated: bad input is stored as given, exactly as before the fix -----------------------

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A role that does not exist is stored as given on import-asyncapi as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_bound_badrole.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "nosuchrole"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A role that does not exist is stored as given on project import as <actor>
    Given I have valid access tokens as "<actor>"
    When I build an API project archive from directory "artifacts/payloads/async/scope-bindings/project_badrole" as "sbArchive"
    And I import the API project archive "sbArchive" with overwrite "false"
    Then The response status code should be 200
    When I find the Publisher API named "{{sbApiName}}" and store its id as "sbApiId"
    And I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "nosuchrole"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:publisher @feat:streaming-design @rule:scope-bindings @type:regression
  Scenario Outline: A role that does not exist is stored as given on PUT asyncapi as <actor>
    Given I have valid access tokens as "<actor>"
    When I import asyncapi definition from "artifacts/payloads/async/scope-bindings/local_bound_admin.yaml" with additional properties "artifacts/payloads/async/scope-bindings/sse_props.json" as "sbApiId"
    Then The response status code should be 201
    When I update the AsyncAPI definition of API "sbApiId" from file "artifacts/payloads/async/scope-bindings/local_bound_badrole.yaml"
    Then The response status code should be 200
    When I retrieve the AsyncAPI definition of API "sbApiId"
    Then The AsyncAPI definition in the response should bind scope "{{sbScope}}" to role "nosuchrole"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
