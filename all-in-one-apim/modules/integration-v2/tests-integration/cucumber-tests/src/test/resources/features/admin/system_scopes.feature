Feature: Admin System Scope Role-Alias Mapping

  Ports APISystemScopesTestCase: the admin REST API for system-scope role-alias mappings
  (/api/am/admin/v4/role-aliases). Adds an alias to a role, confirms it is listed, then clears the mappings.
  Runs ×2 tenant (each tenant maintains its own role-alias mappings). The clear step is the teardown, so no
  mapping is left mutated on the shared container.

  Also hosts the sibling system-scopes lookups: the per-user scope resolution
  (GET system-scopes/{base64(scope)}?username=…) and the role-alias-derived subscriber ownership regression. The
  latter MUST live here rather than in admin/application_owner_change.feature: role-alias mappings are TENANT-GLOBAL
  mutable config and the admin block runs its runners in PARALLEL, so a scenario mutating aliases from another runner
  would race this file's clear-all. Co-locating them in this one runner makes every alias mutation sequential
  (CLAUDE.md §2 — bound to the host's shared mutable fixture). Resources these scenarios create are swept once by
  the runner's AfterClass hook (§5); the feature is deliberately not tagged for per-scenario cleanup.

  @cap:admin @feat:role-scope-mapping @type:regression @legacy:APISystemScopesTestCase
  Scenario Outline: A role-alias scope mapping can be added, retrieved and cleared as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I set the role alias "testRole" for role "admin"
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200
    And The response should contain "testRole"
    When I clear all role aliases
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200
    And The response should not contain "testRole"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports the scope-verification half of OAuthApplicationOwnerUpdateTestCase#updateOwner — the admin
  # GET system-scopes/{base64(scope)}?username=… lookup that decides whether a candidate application owner is a valid
  # subscriber. The suite pins the change-owner 500 for an unknown user but never this endpoint, so nothing proved the
  # subscriber check itself resolves a real user. This POSITIVE case is also the CONTROL for the negative below: a
  # wrong URL or a rejected admin token would 404 there too, so the 404 on its own would prove nothing.
  @cap:admin @feat:role-scope-mapping @rule:scopes-for-user @type:regression @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario Outline: The subscribe scope resolves for an existing subscriber as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the system scope "apim:subscribe" for "<subscriber>"
    Then The response status code should be 200
    And The value of response field "name" should be "apim:subscribe"

    Examples:
      | actor             | subscriber                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # Negative counterpart of the scenario above: the same lookup for a user that does not exist in the store answers
  # exactly 404 (the endpoint's documented "specified resource does not exist"). Distinct from the change-owner
  # negative in admin/application_owner_change.feature, which surfaces the same missing user as a 500.
  @cap:admin @feat:role-scope-mapping @rule:scopes-for-user @type:negative @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario Outline: The subscribe scope lookup for a non-existent user is not found as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve the system scope "apim:subscribe" for the raw user "noSuchUser${UNIQUE:NoUser}"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Ports OAuthApplicationOwnerUpdateTestCase#updateApplicationOwnerWhenHavingCustomRoles — a subscriber whose
  # subscriber-ness comes ONLY from a role ALIAS (not from carrying Internal/subscriber) is a valid application owner.
  # The scenario above covers alias add/list/clear as configuration; this one proves the alias is actually HONOURED by
  # the two consumers that matter: the per-user scope lookup, and change-owner's subscriber validation. Neither user
  # here holds Internal/subscriber — each carries exactly one custom role, aliased onto Internal/subscriber in a
  # SINGLE role-aliases PUT (the PUT replaces the whole mapping list, so two separate calls would discard the first).
  # The custom roles are provisioned with the store login + subscribe permissions (what that provisioning step
  # grants), matching the legacy roles' /permission/admin/manage/api/subscribe. The mapping is cleared at the end;
  # this file's other scenarios also clear it, and every alias mutation in the block is sequential in this runner.
  @cap:admin @feat:role-scope-mapping @rule:alias-subscriber @type:regression @dep:devportal @legacy:OAuthApplicationOwnerUpdateTestCase
  Scenario: An application can be transferred to an owner whose subscriber role is only a role alias
    Given The system is ready
    And I have valid access tokens as "admin"

    # Two custom roles and one user per role — neither user carries Internal/subscriber.
    When I provision store-visibility role "aliasSubRole1" in tenant "carbon.super"
    And I provision store-visibility role "aliasSubRole2" in tenant "carbon.super"
    And I provision user "aliasSubOwner" with roles "aliasSubRole1"
    And I provision user "aliasSubTarget" with roles "aliasSubRole2"

    # Alias BOTH custom roles onto Internal/subscriber in one PUT.
    When I set the role alias "aliasSubRole1,aliasSubRole2" for role "Internal/subscriber"
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200
    And The response should contain "aliasSubRole1"
    And The response should contain "aliasSubRole2"

    # The alias alone makes the transfer target resolve as an apim:subscribe holder.
    When I retrieve the system scope "apim:subscribe" for "aliasSubTarget"
    Then The response status code should be 200
    And The value of response field "name" should be "apim:subscribe"

    # The alias-derived owner can act on the DevPortal and owns an application (its token carries apim:subscribe
    # only because of the alias, so a 201 here already proves the mapping reached token issuance).
    Given I have a valid DCR application as "aliasSubOwner"
    And I have a valid Devportal access token as "aliasSubOwner"
    And I act as "aliasSubOwner"
    When I create an application "${UNIQUE:AliasOwnedApp}" with visibility "PRIVATE" as "aliasOwnedAppId"
    Then The response status code should be 201

    # The target needs its own DevPortal subscriber row before it can receive ownership (see the first scenario of
    # admin/application_owner_change.feature); creating a throwaway application as the target initialises it.
    Given I have a valid DCR application as "aliasSubTarget"
    And I have a valid Devportal access token as "aliasSubTarget"
    And I act as "aliasSubTarget"
    When I create an application "${UNIQUE:AliasTargetSeed}" with visibility "PRIVATE" as "aliasTargetSeedAppId"
    Then The response status code should be 201

    # The transfer to the alias-derived subscriber succeeds and the application lists under it.
    Given I act as "admin"
    When I change the owner of application "aliasOwnedAppId" to "aliasSubTarget"
    Then The response status code should be 200
    When I retrieve the admin applications owned by "aliasSubTarget"
    Then The response status code should be 200
    And The response should contain "{{aliasOwnedAppId}}"

    # Restore the shared (tenant-global) alias configuration.
    When I clear all role aliases
    Then The response status code should be 200
    When I retrieve the role aliases
    Then The response status code should be 200
    And The response should not contain "aliasSubRole1"
