Feature: Search for migrated APIs in dev portal

  Background:
    Given The system is ready and I have valid access tokens for current user

 # Step 1: Search apis in devportal
  Scenario Outline: API search
    When I search DevPortal APIs with query "<query>"
    Then The response status code should be 200
    And The response should contain "<expectedValue>"

    Examples:
    |  query              |  expectedValue|
    |tag:adp-tag          |ADPRestAPI     |
    |name:ADPRestAPI      |ADPRestAPI     |
    |provider:adp_crt_user|ADPRestAPI     |
    |api-category:adp-rest|ADPRestAPI     |

  # Step 2: Find devportal documents
  Scenario Outline: Find devportal documents
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve devportal documents for "<apiID>"
    Then The response status code should be 200
    And The response should contain "adp-inline-doc"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 1.0.0        | RestApiId     |
