@framework
Feature: Framework Verification 5.3 - lifecycle provisioning lands in the freshly booted container

  The block sets initTenantUsers=true, so BlockLifecycleListener provisions the default tenant set into
  the block's own container during onStart (before any class runs). This probe asserts three things: the
  provisioned tenant/user beans are readable from the block's shared scope under the tenant-domain key;
  CURRENT_TENANT resolves off those beans exactly as the publisher runners do; and the tenants/users
  actually exist in the live server - queried back via the same SOAP the legacy init steps use.

  Scenario: Provisioned tenants and users exist in the block container and shared scope
    Then I wait for the APIM server to be ready
    And the shared baseUrl is present

    # Beans landed on the composite block key the probe reads.
    And the shared tenant "carbon.super" has user with key "userKey1"
    And the shared tenant "tenant1.com" has an admin user
    And the shared tenant "tenant1.com" has user with key "userKey1"

    # The provisioned beans are usable downstream (CURRENT_TENANT resolution).
    And I can resolve CURRENT_TENANT for tenant "tenant1.com" and user key "userKey1"

    # The provisioning actually reached the live server (query back via the same SOAP the steps use).
    When I retrieve existing tenant details
    Then the retrieved tenants include "tenant1.com"
    When I retrieve all existing users in the tenant domain "carbon.super"
    Then the retrieved users include "testUser1"
    When I retrieve all existing users in the tenant domain "tenant1.com"
    Then the retrieved users include "testUser11"

    And I record the block observation
