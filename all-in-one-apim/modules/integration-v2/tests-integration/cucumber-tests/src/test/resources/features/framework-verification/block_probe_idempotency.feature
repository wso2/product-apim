@framework
Feature: Framework Verification 5.7 - provisioning is idempotent (skip-if-exists no-ops on re-run)

  The block sets initTenantUsers=true, so BlockLifecycleListener provisions the default tenant set into the
  block's own container during onStart. This probe then provisions the SAME default set a second time and
  asserts the re-run no-ops: it must not throw (a broken skip-if-exists would attempt a re-create and the
  server would answer non-200), and the tenant/user must still exist exactly once on the live server.

  Scenario: Re-provisioning the same tenant set no-ops without error or duplicates
    Then I wait for the APIM server to be ready
    And the shared baseUrl is present

    # Provisioned once by the lifecycle; provisioning the same set again must hit skip-if-exists and no-op.
    And I provision the default tenant set again

    # The re-run created no duplicates - tenant present, user present exactly once on the live server.
    When I retrieve existing tenant details
    Then the retrieved tenants include "tenant1.com"
    When I retrieve all existing users in the tenant domain "tenant1.com"
    Then the retrieved users include "testUser11" exactly once

    And I record the block observation
