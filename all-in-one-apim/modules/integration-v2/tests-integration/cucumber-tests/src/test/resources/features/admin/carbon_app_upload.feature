Feature: Carbon application archive upload
  APIM accepts a CAR archive through CarbonAppUploader and persists its uploaded bytes under the documented carbonapps deployment directory.

  @cap:admin @feat:carbon-app-upload @legacy:CarbonAppUploadTestCase
  Scenario Outline: Upload and verify a CAR as an administrator
    Given I act as "<actor>"
    And the legacy CAR fixture is available
    When I upload the CAR as the acting Carbon administrator
    Then the uploaded CAR should be present in the Carbon applications deployment directory

    Examples:
      | actor             |
      | superTenantAdmin  |
      | admin@tenant1.com |
