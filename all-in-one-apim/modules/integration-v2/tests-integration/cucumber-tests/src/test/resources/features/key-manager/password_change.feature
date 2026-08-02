@cleanup
Feature: DevPortal Self-Service Password Change

  A DevPortal subscriber changes their own password via the self-service endpoint
  (POST api/am/devportal/v3/me/change-password, authorized by the plain devportal scope apim:subscribe),
  then the NEW password issues a token while the OLD password is refused. Framed as key-manager token issuance
  because the meaningful, credential-level proof is a fresh password-grant round trip: the change-password call
  does not rotate the caller's cached token, so re-authentication is what proves the change took effect. Uses a
  throwaway, uniquely-named subscriber provisioned in the test (never the shared subscriberUser, whose password
  is relied on by parallel scenarios). Ports PasswordChangeTestCase. Self-contained; torn down by the
  per-scenario cleanup hook.

  @cap:key-manager @feat:token-issuance @type:regression @legacy:PasswordChangeTestCase
  Scenario: A subscriber changes their password and re-authenticates with the new one
    Given The system is ready
    When I provision a throwaway subscriber "throwawaySub" in tenant "carbon.super" with password "Password123!"
    And The system is ready and I have valid devportal access token as "throwawaySub"
    When I change the acting user's password from "Password123!" to "NewPass456!" via the DevPortal
    Then The response status code should be 200
    # The NEW password issues a token; the OLD password is refused. The refusal surfaces at the DCR basic-auth
    # gate of the password-grant round trip, which returns 401 "Unauthenticated request" (not a 400 invalid_grant).
    Then a password-grant token request for the acting user with password "NewPass456!" returns status 200
    And a password-grant token request for the acting user with password "Password123!" returns status 401
