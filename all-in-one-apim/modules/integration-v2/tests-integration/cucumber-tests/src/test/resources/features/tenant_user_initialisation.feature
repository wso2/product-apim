#Feature: Tenant, Role and User Initialization
#
#  Scenario: Add tenants
#    Given the API Manager is running
#    When I add tenant "First" "Tenant" with admin username "tenant1" and admin password "tenant1" with domain "tenant1.com" and email "admin@tenant1.com"
##    And I add tenant "Second" "Tenant" with admin username "tenant2" and admin password "tenant2" with domain "tenant2.com" and email "admin@tenant2.com"
#    Then the tenants should be added successfully
#
#  Scenario: Add roles
##    When I add role "test_role_super" to tenant "superTenant"
##    And I add role "test_role_1" to tenant "tenant1.com"
##    And I add role "test_role_2" to tenant "tenant2.com"
#    Then the roles should be added successfully
#
#  Scenario: Add users with specific roles
#    When I add user "user1" with password "user1" to tenant "superTenant" with roles "admin,Internal/analytics"
#    And I add user "user2" with password "user2" to tenant "superTenant" with roles "Internal/analytics,test_role_super,Internal/system"
#    And I add user "user3" with password "user3" to tenant "tenant1.com" with roles "admin,Internal/analytics,Internal/integration_dev,Internal/publisher"
#    And I add user "user4" with password "user4" to tenant "tenant1.com" with roles "test_role_1"
##    And I add user "user5" with password "user5" to tenant "tenant2.com" with roles "Internal/analytics,Internal/integration_dev,Internal/publisher,Internal/creator,Internal/system"
##    And I add user "user6" with password "user6" to tenant "tenant2.com" with roles "admin,test_role_2"
#    Then the users should be added successfully with roles
