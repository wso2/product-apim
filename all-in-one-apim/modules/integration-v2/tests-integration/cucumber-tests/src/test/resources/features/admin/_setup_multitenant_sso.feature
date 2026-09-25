@setup
Feature: Setup - multi-tenant console SSO broker topology

  Provisions the nested-OIDC broker that lets a tenant's user authenticate at an external Identity Server and
  land in an API Manager console: the tenant (created in the identity server and synchronized to API Manager),
  its identity server application, user and groups, the tenant's OIDC identity provider and common service
  provider, the super tenant's broker identity provider, and the Publisher console wired to that broker.

  Asserts only that each provisioning call succeeds. The identity artifacts must outlive this feature for the
  journey that follows, so this file is deliberately NOT tagged @cleanup - the runner's AfterClass sweep tears
  the block down. Listed first by its _setup_ prefix so it runs before the journey.

  Scenario: Provision the multi-tenant SSO broker topology
    Given The system is ready
    And I have valid access tokens as "admin"
    # The multi-tenant SSO guide connects the identity server as a key manager, so the journey runs against
    # the documented configuration rather than a simpler one.
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "mtSsoKm"
    Then The response status code should be 201
    When I provision the SSO tenant "abc.com" on the identity server with admin password "Admin@12345"
    And I provision the SSO identity server application in tenant "abc.com"
    And I provision the SSO user "ssouser" with password "Test@12345" in groups "creator,publisher,subscriber,admin" in tenant "abc.com"
    And I configure the SSO tenant identity provider in tenant "abc.com"
    And I configure the SSO common service provider in tenant "abc.com"
    And I configure the SSO multi-tenant broker identity provider
    And I configure the SSO common service provider in the super tenant
    And I initialize all console service providers
    And I assert all console service providers exist before wiring them
    And I wire the "publisher" console to the multi-tenant broker
    And I wire the "admin" console to the multi-tenant broker
    And I wire the "devportal" console to the multi-tenant broker
