@cap:admin @feat:external-key-manager @rule:roles @type:regression
Feature: External Key Manager Role Creation

  Verifies that the WSO2-IS-7 key manager, registered with enable_roles_creation=true (wso2is7.json), creates
  roles in WSO2 Identity Server at runtime. When a shared scope bound to a new role is registered in APIM, the
  IS7 connector's registerScope creates the corresponding role in IS via the SCIM2 Roles API. The connector maps
  a plain APIM role r to the IS role system_primary_r (which is why the IS deployment.toml sets
  [role_mgt] allow_system_prefix_for_role = true). This scenario creates a shared scope bound to a uniquely-named
  role and then asserts the derived role now exists in IS by querying IS's SCIM2 Roles API directly - the
  authoritative check, since APIProviderImpl.addSharedScope swallows a key-manager registerScope failure and
  returns 201 either way. The key manager is registered by the runner-shared _setup_is7_key_manager fixture; the feature is therefore
  NOT per-scenario @cleanup (that sweep would delete the fixture KM) - the runner's AfterClass sweep removes
  the scope and KM once, and the created IS role vanishes with the ephemeral IS container at block teardown.

  Scenario: A shared scope bound to a new role creates that role in IS7
    Given The system is ready and I have valid publisher access tokens as "admin"
    When I create a shared scope bound to a new IS7 role, storing the expected IS7 role name as "expectedIs7Role"
    Then The response status code should be 201
    And the role stored as "expectedIs7Role" should exist at the external key manager
