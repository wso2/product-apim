@cleanup
Feature: Multi-tenant console SSO via an external Identity Server

  A tenant's users are not held in API Manager - they live in the organisation's own Identity Server. This
  feature covers that user signing in to a console.

  The console's service provider belongs to the super tenant, so an identity provider attached to it directly
  would resolve every federated login into the super tenant. The multi-tenant broker exists for exactly that
  reason: it serves a tenant-selection page first, then hands the flow to the chosen tenant's own service
  provider, which federates to the Identity Server application in that tenant. So the tenant is decided before
  any credentials are entered, and the resulting session belongs to the tenant.

  Driven in a REAL headless browser against the actual console, like the single-tenant console-SSO journey: the
  broker's tenant-selection page and the identity server's login form are both real pages a user interacts
  with, and a browser is what proves they are usable.

  The assertion that matters is WHICH principal the session ends up as. Landing in the console proves only that
  somebody authenticated; a super-tenant principal there would mean the broker chain silently collapsed to an
  ordinary federated login, which is the failure this feature exists to catch.



  @cap:admin @feat:sso @rule:multitenant-sso @type:regression
  Scenario: A tenant user signs in to the Publisher through the multi-tenant broker

    Given a real browser session for the multi-tenant SSO journey
    When I open the "publisher" console login page for multi-tenant SSO
    Then the multi-tenant console login page must carry "multiOptionURI" and offer local plus broker authentication
    When I select the multi-tenant broker federated authenticator
    And I select the tenant "abc.com" on the broker's tenant-selection page
    And I authenticate at the identity server as the tenant's SSO user
    Then I should land authenticated in the "publisher" console as a tenant user
    And the "publisher" console session should belong to the tenant's SSO user
    # Role-gated work is the proof that matters. Creating an API requires a creator role, so it only
    # succeeds if the identity server groups reached API Manager as roles; and the provider recorded for
    # it carries the tenant, which is what makes this multi-tenant rather than a federated login that
    # happens to work.
    And I report where the federated user landed and what roles it holds
    And I create a REST API via the publisher UI as the tenant user named "MT_SSO_API"
    Then the created API should be owned by the tenant's SSO user in tenant "abc.com"
    And I deploy and publish the created API through the publisher UI
    When I open the "devportal" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "devportal" console as a tenant user
    And the "devportal" console session should belong to the tenant's SSO user
    And I create a DevPortal application named "MT_SSO_APP" as the tenant user
    And I subscribe the DevPortal application to the created API
    And I generate production keys for the DevPortal application
    Then I invoke the created API through the gateway using the DevPortal application
    When I open the "admin" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "admin" console as a tenant user
    And the "admin" console session should belong to the tenant's SSO user
    # Changing an API provider is an admin-plane operation. It is the authorization checkpoint: a successful
    # Admin-console landing alone would not prove that the federated user's external admin group was mapped to
    # API Manager's tenant role "admin".
    And I change the created API provider to the tenant admin through the admin console
    When I log out of the "publisher" console for multi-tenant SSO
    Then the "publisher" multi-tenant logout should return through its callback
    And no multi-tenant console session cookies should remain
    And the "publisher" console must demand a fresh login after multi-tenant logout
    And the Admin console must demand a fresh login after multi-tenant logout
    And the DevPortal session must no longer authorize the MT application

  @cap:admin @feat:sso @rule:multitenant-sso @type:regression
  Scenario: A tenant user starts in Admin and hands off to Publisher and DevPortal

    Given a real browser session for the multi-tenant SSO journey
    When I open the "admin" console login page for multi-tenant SSO
    Then the multi-tenant console login page must carry "multiOptionURI" and offer local plus broker authentication
    When I select the multi-tenant broker federated authenticator
    And I select the tenant "abc.com" on the broker's tenant-selection page
    And I authenticate at the identity server as the tenant's SSO user
    Then I should land authenticated in the "admin" console as a tenant user
    And the "admin" console session should belong to the tenant's SSO user
    When I open the "publisher" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "publisher" console as a tenant user
    And the "publisher" console session should belong to the tenant's SSO user
    And I create a REST API via the publisher UI as the tenant user named "MT_SSO_ADMIN_FIRST_API"
    Then the created API should be owned by the tenant's SSO user in tenant "abc.com"
    And I deploy and publish the created API through the publisher UI
    When I open the "devportal" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "devportal" console as a tenant user
    And the "devportal" console session should belong to the tenant's SSO user
    And I create a DevPortal application named "MT_SSO_ADMIN_FIRST_APP" as the tenant user
    And I subscribe the DevPortal application to the created API
    And I generate production keys for the DevPortal application
    Then I invoke the created API through the gateway using the DevPortal application
    When I log out of the "admin" console for multi-tenant SSO
    Then the "admin" multi-tenant logout should return through its callback
    And no multi-tenant console session cookies should remain
    And the "admin" console must demand a fresh login after multi-tenant logout
    And the "publisher" console must demand a fresh login after multi-tenant logout
    And the DevPortal session must no longer authorize the MT application

  @cap:admin @feat:sso @rule:multitenant-sso @type:regression
  Scenario: A tenant user starts in DevPortal and hands off to Publisher and Admin

    Given a real browser session for the multi-tenant SSO journey
    When I open the "devportal" console login page for multi-tenant SSO
    Then the multi-tenant console login page must carry "multiOptionURI" and offer local plus broker authentication
    When I select the multi-tenant broker federated authenticator
    And I select the tenant "abc.com" on the broker's tenant-selection page
    And I authenticate at the identity server as the tenant's SSO user
    Then I should land authenticated in the "devportal" console as a tenant user
    And the "devportal" console session should belong to the tenant's SSO user
    When I open the "publisher" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "publisher" console as a tenant user
    And the "publisher" console session should belong to the tenant's SSO user
    And I create a REST API via the publisher UI as the tenant user named "MT_SSO_DEVPORTAL_FIRST_API"
    Then the created API should be owned by the tenant's SSO user in tenant "abc.com"
    And I deploy and publish the created API through the publisher UI
    When I open the "devportal" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "devportal" console as a tenant user
    And the "devportal" console session should belong to the tenant's SSO user
    And I create a DevPortal application named "MT_SSO_DEVPORTAL_FIRST_APP" as the tenant user
    And I subscribe the DevPortal application to the created API
    And I generate production keys for the DevPortal application
    Then I invoke the created API through the gateway using the DevPortal application
    When I open the "admin" console expecting multi-tenant SSO
    Then I should not be prompted to log in again for multi-tenant SSO
    And I should land authenticated in the "admin" console as a tenant user
    And the "admin" console session should belong to the tenant's SSO user
    And I change the created API provider to the tenant admin through the admin console
    When I log out of the "devportal" console for multi-tenant SSO
    Then the "devportal" multi-tenant logout should return through its callback
    And no multi-tenant console session cookies should remain
    And the "publisher" console must demand a fresh login after multi-tenant logout
    And the Admin console must demand a fresh login after multi-tenant logout
    And the DevPortal session must no longer authorize the MT application

  @cap:admin @feat:sso @rule:multitenant-sso @type:regression
  Scenario Outline: The MT multi-option login still authenticates a local administrator in <tenant>

    Given a real browser session for the multi-tenant SSO journey
    When I open the "publisher" console login page for multi-tenant SSO
    Then the multi-tenant console login page must carry "multiOptionURI" and offer local plus broker authentication
    When I authenticate with the local authenticator as the "<tenant>" tenant administrator
    Then I should land authenticated in the "publisher" console as a tenant user
    And the "publisher" console session should belong to the local "<tenant>" tenant administrator

    Examples:
      | tenant       |
      | carbon.super |
      | abc.com      |
