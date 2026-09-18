@cleanup
Feature: API Provider Change To An Email-Form Username

  Handing an API over to a provider whose USERNAME IS AN EMAIL ADDRESS. The change-provider DAO deliberately
  round-trips both the old and the new provider value through the product's e-mail-domain replacement before
  writing them, so an e-mail-shaped username is a real code branch of this operation and not a data variation of
  the plain-username transfer in publisher/api_provider_change.

  WHY THIS LIVES IN ITS OWN FILE RATHER THAN AS EXAMPLES ROWS ON api_provider_change.feature. The dimension is a
  CONTAINER MODE, not an actor: the block sets emailUserMode, which changes the physical username of every
  provisioned user, and its overlay turns on enable_email_domain, which changes how the whole container resolves
  an at-sign in ANY username. A sibling runner expecting plain actor usernames could not survive it. Hence a
  separate feature riding the IntegrationV2-EmailUserName block, as the other email-username features do.

  WHAT MAKES THE ASSERTION DISCRIMINATING. A 200 on the transfer would pass even if the principal were silently
  truncated at its first at-sign, because the API would still be re-owned - by SOMEONE. Two things are pinned
  instead: the target's EXACT physical username, before the transfer runs, so a block that quietly provisioned
  plain usernames cannot make this feature pass; and the stored provider read back from the publisher plane,
  which must equal that same e-mail-form principal. The API's identity triple is re-read alongside it, because
  a transfer that mangled the owner while leaving the API addressable is the failure worth catching.
  Teardown via the per-scenario cleanup hook.

  # scenario: KB-APIM-0001-S11
  @cap:publisher @feat:api-lifecycle @rule:email-username @type:regression @dep:admin @legacy:ChangeApiProviderTestCase
  Scenario Outline: An API is handed to a provider whose username is an email address as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I provision user "emailNewProvider" with roles "Internal/creator,Internal/publisher" in tenant "<tenant>"

    # Pin the target's PHYSICAL username FIRST. If the block ever provisioned plain usernames, everything below
    # would still pass and this feature would be testing nothing.
    And I act as "emailNewProvider<suffix>"
    And I store the acting actor credentials as "emailNewProviderName" and "emailNewProviderPassword"
    Then the actual value of "emailNewProviderName" should match the expected value:
      """
      <physicalUsername>
      """

    When I act as "<actor>"
    And I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "epcApiPayload"
    And I create an "apis" resource with payload "epcApiPayload" as "epcApiId"
    Then The response status code should be 201
    When I retrieve the "apis" resource with id "epcApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "epcApiName"
    And I extract response field "context" and store it as "epcApiContext"
    And I extract response field "version" and store it as "epcApiVersion"

    # Hand the API over, naming the e-mail-shaped username exactly as the caller holds it. That form is the one
    # the publisher plane reports a provider in - a tenant user stays fully qualified, a super-tenant user drops
    # the "@carbon.super" suffix - which is why it differs from the physical username pinned above for the
    # super-tenant row and is identical to it for the tenant row.
    When I change the provider of API "epcApiId" to "<suppliedUsername>"
    Then The response status code should be 200

    # The stored provider is that same e-mail-shaped principal, not a domain-mangled or truncated variant, and the
    # API is still addressable by the same id under an unchanged identity triple.
    And The provider of API "epcApiId" should match actor "emailNewProvider<suffix>"
    When I retrieve the "apis" resource with id "epcApiId"
    Then The response status code should be 200
    And The value of response field "name" should be "{{epcApiName}}"
    And The value of response field "context" should be "{{epcApiContext}}"
    And The value of response field "version" should be "{{epcApiVersion}}"

    Examples:
      | actor             | tenant       | suffix       | physicalUsername                        | suppliedUsername                       |
      | admin             | carbon.super |              | emailNewProvider@email.com@carbon.super | emailNewProvider@email.com             |
      | admin@tenant1.com | tenant1.com  | @tenant1.com | emailNewProvider@email.com@tenant1.com  | emailNewProvider@email.com@tenant1.com |
