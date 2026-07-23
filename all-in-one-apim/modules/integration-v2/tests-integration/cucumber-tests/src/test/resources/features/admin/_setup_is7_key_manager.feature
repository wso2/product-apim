@setup
Feature: Setup - WSO2 IS 7.x external key manager registration

  Registers the block-booted WSO2 Identity Server 7.x as a third-party key manager via the admin REST API - the
  common prerequisite fixture for runners whose scenarios need a registered external KM but do not test the
  registration itself (that arc lives in the end-to-end token-flow feature). Runs once per runner (listed first
  by the _setup_ prefix); the created key manager id/name land in the runner's local scope as "is7Km" /
  "is7KmName" for later keygen payloads. Swept by the runner's AfterClass cleanup (applications delete before
  key managers, so the KM delete is FK-safe). Requires the block to boot IS first (bootExternalIdentityServer).

  Scenario: Register the external WSO2 IS 7.x key manager
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a key manager from payload "artifacts/payloads/keymanagers/wso2is7.json" as "is7Km"
    Then The response status code should be 201
