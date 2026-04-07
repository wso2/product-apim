Feature: Migrated Tenant and User Initialization

  Scenario: Add tenants
    And I add adpsample tenant to context

  Scenario: Add users
   # Retrieve migrated tenant users and add a new user with previous roles
    When I retrieve all existing users in the tenant domain "adpsample.com"
    And I add user "userKey1" with username "testTenantUser11", password "testTenantUser11" and roles "ADP_CREATOR, ADP_PUBLISHER, ADP_SUBSCRIBER" to the tenant domain "adpsample.com"

  Scenario: Add migrated subscriber users
    # Register migrated subscriber user (adp_sub_user) in super tenant context
    When I register existing user "subscriberKey1" with username "adp_sub_user" and password "adp_sub_user" in tenant context "carbon.super"
    # Register migrated subscriber user (adp_sub_user) in adpsample tenant context
    And I register existing user "subscriberKey1" with username "adp_sub_user" and password "adp_sub_user" in tenant context "adpsample.com"

