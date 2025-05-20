Feature: Complete API Lifecycle Management

  Background:
    Given I have initialized the NodeApp server container
    And I have initialized the Default API Manager container
#    Given I have initialized test instance
    And I initialize the Publisher REST API client with username "admin", password "admin" and tenant "carbon.super"
    And I initialize the Store REST API client with username "admin", password "admin" and tenant "carbon.super"

  Scenario: End-to-End API Lifecycle from Creation to Invocation
    When I create an API with the following details
      | name                | CustomerServiceAPI                                  |
      | context             | /jaxrs                                              |
      | version             | 1.0.0                                               |
      | apiEndpointURL      | jaxrs_basic/services/customers/customerservice/     |
      | tiersCollection     | Gold,Bronze,Unlimited                               |
      | tier                | Gold                                                |

    And I update API of id "<createdApiId>" with the following details

      | description         | Simple Customer Service API                         |
      | tags                | customer,service                                    |
      | defaultVersion      | true                                                |
      | securitySchemes     | oauth2,basic,api_key                                |
      | businessOwner       | Jane Roe                                            |
      | businessOwnerEmail  | marketing@jaxrs.com                                 |
      | technicalOwner      | John Doe                                            |
      | technicalOwnerEmail | architecture@jaxrs.com                              |
      | operations          | [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"Unlimited"},{"target":"/order","verb":"POST","authType":"Application","throttlingPolicy":"Unlimited"}] |

#    And I add an operation with the following details to the created API with id "<createdApiId>"
#      | target              | Simple Customer Service API                         |
#      | verb                | customer,service                                    |
#      | authType            | true                                                |
#      | throttlingPolicy    | oauth2,basic,api_key                                |

    And I deploy a revision of the API with id "<createdApiId>"
    Then I should be able to retrieve the API with id "<createdApiId>"

    When I publish the API with id "<createdApiId>"
    Then The lifecycle status of API "<createdApiId>" should be "Published"

    When I create an application with the following details
      | name             | CustomerApp            |
      | throttlingPolicy | Unlimited              |
      | description      | Test app for scenarios |

    Then I should be able to retrieve the application with id "<createdAppId>"

    When I subscribe to API "<createdApiId>" using application "<createdAppId>" with throttling policy "Bronze"
    Then I should be able to retrieve the subscription for Api "<createdApiId>" by Application "<createdAppId>"


    When I generate client credentials for application id "<createdAppId>" with key type "PRODUCTION"
    And I request an access token using grant type "client_credentials" without any scope
    And I invoke API of ID "<createdApiId>" with path "/customers/123/" and method GET using access token "<generatedAccessToken>"
    Then the API response status should be 200

