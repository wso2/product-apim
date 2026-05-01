Feature: Search for migrated APIs in dev portal

  Background:
    Given The system is ready and I have valid access tokens for current user

 # Step 1: Search apis in devportal
  Scenario Outline: API search
    When I search DevPortal APIs with query "<query>"
    And I wait until the response status code is 200
    And The response should contain "<expectedValue>"

    Examples:
    |  query              |  expectedValue|
    |tag:adp-tag          |ADPRestAPI     |
    |name:ADPRestAPI      |ADPRestAPI     |
    |provider:adp_crt_user|ADPRestAPI     |
    |api-category:adp-rest|ADPRestAPI     |

  # Step 2: Find devportal documents
  Scenario Outline: Find devportal documents
    When I find the API created with the name "<apiName>" and version "<apiVersion>"
    And I wait until the response status code is 200
    And I extract response field "count" and store it as "<apiCount>"
    And the actual value of "<apiCount>" should match the expected value:
      """
      1
      """
    And I extract response field "list[0].id" and store it as "<apiID>"

    And I retrieve devportal documents for "<apiID>"
    And I wait until the response status code is 200
    And The response should contain "adp-inline-doc"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 1.0.0        | RestApiId     |
