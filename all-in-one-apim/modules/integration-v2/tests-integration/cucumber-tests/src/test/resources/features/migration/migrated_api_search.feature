Feature: Search for migrated APIs in dev portal

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: API search
    When I search DevPortal APIs with query "<query>"
    Then The response status code should be 200
    And The response should contain "<expectedValue>"
    And The response should contain "<count>"

    Examples:
    |  query              |  expectedValue|  count      |
    |tag:adp-tag          |ADPRestAPI     | \"count\":1 |
    |name:ADPRestAPI      |ADPRestAPI     | \"count\":1 |
    |version:1.0.0        |ADPRestAPI     | \"count\":9 |
    |provider:adp_crt_user|ADPRestAPI     | \"count\":7 |
    |api-category:adp-rest|ADPRestAPI     | \"count\":4 |

  Scenario Outline: Find devportal documents
    When I find the apiUUID of the API created with the name "<apiName>" and version "<apiVersion>" as "<apiID>"
    And I retrieve devportal documents for "<apiID>"
    Then The response status code should be 200
    And The response should contain "adp-inline-doc"

    Examples:
      | apiName                  | apiVersion   | apiID         |
      | ADPRestAPI               | 1.0.0        | RestApiId     |


