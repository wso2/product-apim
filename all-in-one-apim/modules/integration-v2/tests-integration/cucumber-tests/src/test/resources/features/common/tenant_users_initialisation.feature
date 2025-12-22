Feature: Tenant and User Initialization

  Scenario: Add tenants
    And I add super tenant to context
    And I add adpsample tenant to context

  Scenario: Add tenants
    When I retrieve existing tenant details
    And I add a new tenant with the following details
      | First Name      | First               |
      | Last Name       | Tenant              |
      | Admin Username  | admin               |
      | Admin Password  | admin               |
      | Domain          | tenant1.com         |
      | Email           | admin@tenant1.com   |

  Scenario: Add users
    # Create super tenant user
    When I retrieve all existing users in the tenant domain "carbon.super"
    And I add user "userKey1" with username "testUser1", password "testUser1" and roles "Internal/creator, Internal/publisher, Internal/subscriber" to the tenant domain "carbon.super"
    # Create tenant user
    When I retrieve all existing users in the tenant domain "tenant1.com"
    And I add user "userKey1" with username "testUser11", password "testUser11" and roles "Internal/creator, Internal/publisher, Internal/subscriber" to the tenant domain "tenant1.com"

    When I retrieve all existing users in the tenant domain "adpsample.com"
#    And I add user "userKey1" with username "testUser111", password "testUser111" and roles "Internal/creator, Internal/publisher, Internal/subscriber" to the tenant domain "adpsample.com"


