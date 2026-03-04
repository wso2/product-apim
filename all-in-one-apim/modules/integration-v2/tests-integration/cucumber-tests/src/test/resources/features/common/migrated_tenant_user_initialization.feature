Feature: Migrated Tenant and User Initialization

  Scenario: Add tenants
    And I add adpsample tenant to context

  Scenario: Add users
   # Retrieve migrated tenant users and add a new user with previous roles
    When I retrieve all existing users in the tenant domain "adpsample.com"
    And I add user "userKey1" with username "testTenantUser11", password "testTenantUser11" and roles "ADP_CREATOR, ADP_PUBLISHER, ADP_SUBSCRIBER" to the tenant domain "adpsample.com"
