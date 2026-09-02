@cleanup
Feature: Console SSO via an external Identity Server survives the multi-option OAuth2 scope header

  APIM's Publisher, Admin and DevPortal consoles authenticate through APIM's resident IS. When a console
  service provider's first authentication step offers BOTH a local option (BasicAuthenticator) AND a
  federated external IdP, the authentication framework emits a "multiOptionURI" query parameter on the
  /commonauth request. That parameter carries the service provider's full OAuth2 scope list DOUBLE-encoded;
  on a build whose Tomcat maxHttpHeaderSize is too small for the (grown) scope list, Tomcat rejects
  /commonauth with 400 BEFORE any WSO2 code runs, and the SSO login can never complete
  (regression wso2/api-manager #17744).

  This feature drives the journey in a REAL headless browser (Playwright Chromium) against the actual console
  SPAs, reaching the mapped container ports through an in-process CONNECT proxy so the servers' own
  redirect_uri hosts stay consistent end to end. A real browser is load-bearing here: it sends a full browser
  header set, so the #17744 condition - the multi-option /commonauth request whose expanded scope list
  overruns a too-small maxHttpHeaderSize - is exercised naturally (a lean HTTP client sends far fewer header
  bytes, stays under the limit, and would log in even on a broken build: a false green).

  A federated user completes the multi-option login to the Publisher, then does GENUINE role-specific work
  across all three consoles under the ONE federated session, each opened WITHOUT a second login prompt - which
  is the real single-sign-on proof (a user sees "no relogin" and gets work done, not cookies):
    - Publisher: create, deploy and publish a REST API (real SPA clicks).
    - Admin: change the API's provider. Provider change has no Admin-console UI, so it is performed with the
      SSO session's OWN admin token (reconstructed from the split-token cookies), proving the federated admin
      session authorizes a real admin-only API call.
    - Publisher again (same session, no relogin): redeploy the API.
    - DevPortal: create an application, subscribe it to the API, generate keys, and INVOKE the API through the
      gateway with an app token minted from those keys - an end-to-end consumer proof (HTTP 200 from the
      backend).

  The journey then ends with SINGLE LOGOUT: one console logout must leave no usable session in any of the three
  consoles, asserted both behaviourally (each console demands a fresh login) and as cookie residue (no
  resident-IS or console token cookie survives to keep authorizing calls). Logout is a distinct behaviour, so
  by CLAUDE.md section 6 it would normally be its own scenario; it is the journey's final act here because it
  needs an established cross-console SSO session, and a separate scenario would have to repeat the entire
  federated login to build one.

  On a build carrying the maxHttpHeaderSize fix the whole journey completes; on a build without it the very
  first federated /commonauth request is rejected and the Publisher login never lands - failing this test.
  Scope: single-tenant (carbon.super), the standard OpenIDConnectAuthenticator federation path.

  Background:
    Given The system is ready
    And an external Identity Server is registered as OIDC identity provider "ISIDP"
    And the "publisher", "admin" and "devportal" consoles each offer a multi-option login step with a local authenticator and "ISIDP"
    And a federated user "sso_admin" exists on the external Identity Server with the "admin", "publisher" and "subscriber" roles
    And a real browser session for the console SSO journey

  @cap:admin @feat:sso @rule:external-idp-sso @type:regression @dep:publisher @dep:devportal @legacy:APIMANAGER-17744
  Scenario: A federated user single-signs-on across the Publisher, Admin and DevPortal and does real work in each

    # ---- Federated multi-option login to the Publisher (this is the #17744 /commonauth path) ----
    When I open the "publisher" console login page
    # Fixture-integrity guards (NOT the assertion): the bug only exists when the step is multi-option, so if
    # multiOptionURI is absent the fixture has silently degraded and the test would pass for the wrong reason.
    Then the login page must be multi-option carrying "multiOptionURI"
    And the login page must offer both a local authenticator and the "ISIDP" federated authenticator
    When I select the "ISIDP" federated identity provider
    And I authenticate at the external Identity Server as "sso_admin"
    Then I should land authenticated in the "publisher" console
    # Real Publisher work under the SSO session: create, deploy and publish an API.
    And I create a REST API via the publisher UI named "SSO_API"
    And I deploy the API via the publisher UI
    And I publish the API via the publisher UI

    # ---- Genuine single sign-on: opening the other consoles must NOT re-prompt for login ----
    #      (the shared IS session is reused via the same cookie jar, so no login.do page is shown)
    When I open the "admin" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "admin" console
    And I change the API provider to actor "publisherUser" using the admin SSO session
    Then the API provider must be "publisherUser"

    # ---- Back in the Publisher with the SAME session (no second login): redeploy the API ----
    And I redeploy the API via the publisher UI

    When I open the "devportal" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "devportal" console
    And I create an application via the devportal UI named "SSO_App"
    And I subscribe the application to the API via the devportal UI
    And I generate keys for the application via the devportal UI
    And I invoke the API through the gateway using the generated application key

    # ---- Single logout: ONE logout must leave no usable session in ANY of the three consoles ----
    #      Logging out of one console drives the resident-IS OIDC logout and the federated IdP's logout, so a
    #      session surviving in another console would mean the logout was local rather than global. Both halves
    #      are asserted: each console must demand a fresh login, AND no session cookie may survive to keep
    #      authorizing calls after the interactive session appears gone.
    #      The DevPortal is checked differently, and deliberately: it has no server-side login entry to redirect
    #      to (its /login route is served by the SPA, and /services/auth/login - which the Publisher and Admin
    #      consoles map - is a 404 there) and it allows anonymous browsing, so "a login page appeared" is not a
    #      meaningful signal. Its session is proven dead by the stronger check instead: the token no longer
    #      authorizes the application it created moments earlier.
    When I log out of the "publisher" console
    Then the "publisher" console logout should return through its callback
    Then the "publisher" console must demand a fresh login
    And the "admin" console must demand a fresh login
    And the "devportal" console session must no longer authorize its application
    And no console session cookies remain

  @cap:admin @feat:sso @rule:external-idp-sso @type:regression @dep:publisher @dep:devportal
  Scenario: A federated user starts in Admin, uses Publisher and DevPortal, then logs out from Admin

    When I open the "admin" console login page
    Then the login page must be multi-option carrying "multiOptionURI"
    And the login page must offer both a local authenticator and the "ISIDP" federated authenticator
    When I select the "ISIDP" federated identity provider
    And I authenticate at the external Identity Server as "sso_admin"
    Then I should land authenticated in the "admin" console
    When I open the "publisher" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "publisher" console
    And I create a REST API via the publisher UI named "SSO_ADMIN_FIRST_API"
    And I deploy the API via the publisher UI
    And I publish the API via the publisher UI
    When I open the "devportal" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "devportal" console
    And I create an application via the devportal UI named "SSO_ADMIN_FIRST_App"
    And I subscribe the application to the API via the devportal UI
    And I generate keys for the application via the devportal UI
    And I invoke the API through the gateway using the generated application key
    When I log out of the "admin" console
    Then the "admin" console logout should return through its callback
    And the "admin" console must demand a fresh login
    And the "publisher" console must demand a fresh login
    And the "devportal" console session must no longer authorize its application
    And no console session cookies remain

  @cap:admin @feat:sso @rule:external-idp-sso @type:regression @dep:publisher @dep:devportal
  Scenario: A federated user starts in DevPortal, uses Publisher and Admin, then logs out from DevPortal

    When I open the "devportal" console login page
    Then the login page must be multi-option carrying "multiOptionURI"
    Then the login page must offer both a local authenticator and the "ISIDP" federated authenticator
    When I select the "ISIDP" federated identity provider
    And I authenticate at the external Identity Server as "sso_admin"
    Then I should land authenticated in the "devportal" console
    When I open the "publisher" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "publisher" console
    And I create a REST API via the publisher UI named "SSO_DEVPORTAL_FIRST_API"
    And I deploy the API via the publisher UI
    And I publish the API via the publisher UI
    When I open the "devportal" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "devportal" console
    And I create an application via the devportal UI named "SSO_DEVPORTAL_FIRST_App"
    And I subscribe the application to the API via the devportal UI
    And I generate keys for the application via the devportal UI
    And I invoke the API through the gateway using the generated application key
    When I open the "admin" console
    Then I should NOT be prompted to log in again
    And I should land authenticated in the "admin" console
    And I change the API provider to actor "publisherUser" using the admin SSO session
    Then the API provider must be "publisherUser"
    When I log out of the "devportal" console
    Then the "devportal" console logout should return through its callback
    And the "publisher" console must demand a fresh login
    And the "admin" console must demand a fresh login
    And the "devportal" console session must no longer authorize its application
    And no console session cookies remain

  @cap:admin @feat:sso @rule:external-idp-sso @type:regression
  Scenario Outline: The multi-option login still authenticates a LOCAL user, in both tenants, as <actor>

    # The other half of the multi-option step. Adding the federated option is what makes this step multi-option
    # in the first place, so local login is exactly what it can break - and nothing else covers it.
    #
    # This is where the tenant dimension is real. The console service provider is a super-tenant SaaS app, and
    # BasicAuthenticator resolves a QUALIFIED USERNAME against that tenant's user store, so admin@tenant1.com
    # authenticates as the tenant's admin through the very same service provider. The federated option cannot be
    # fanned out this way: it resolves through the identity provider and always lands in the provider's own
    # tenant, which is why only this scenario carries an actor column.
    When I open the "publisher" console login page
    Then the login page must be multi-option carrying "multiOptionURI"
    And the login page must offer both a local authenticator and the "ISIDP" federated authenticator
    When I log in with the local authenticator as "<actor>"
    Then I should land authenticated in the "publisher" console
    And the "publisher" console session should belong to "<actor>"

    Examples:
      | actor              |
      | admin              |
      | admin@tenant1.com  |
