
Feature: Tenant and User Initialization

  Scenario: Add tenants
    When I retrieve existing tenant details
    And I add a new tenant with the following details
      | First Name      | First               |
      | Last Name       | Tenant              |
      | Admin Username  | admin1              |
      | Admin Password  | adminPassword1      |
      | Domain          | tenant1.com         |
      | Email           | admin@tenant1.com   |

  Scenario: Add users
    # Create super tenant user
    When I retrieve all existing users in the tenant using tenant admin username "admin" and password "admin"
    And I add user "user1" with password "userPassword1" and roles "Internal/creator, Internal/publisher, Internal/subscriber" using tenant admin username "admin" and password "admin"
    # Create tenant user
    When I retrieve all existing users in the tenant using tenant admin username "admin1@tenant1.com" and password "adminPassword1"
    And I add user "user2" with password "userPassword2" and roles "Internal/creator, Internal/publisher, Internal/subscriber" using tenant admin username "admin1@tenant1.com" and password "adminPassword1"
