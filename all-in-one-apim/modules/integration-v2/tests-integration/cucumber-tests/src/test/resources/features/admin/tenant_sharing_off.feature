@cap:admin @feat:external-key-manager @rule:tenant-sharing @type:negative
Feature: External Key Manager Tenant Sharing Disabled

  With [apim.key_manager] skip_create_resident_key_manager=true but tenant-sharing OFF (no [[apim.tenant_sharing]]),
  a tenant provisioned through the notify endpoint is created with NO key manager - neither a Resident KM (skipped)
  nor an auto-configured WSO2-IS-7 KM (tenant-sharing off). Application key generation is then refused with
  901403 "Key Manager not Registered". This pins the documented dependency: skipping the Resident KM REQUIRES
  tenant-sharing to supply a WSO2-IS-7 KM, else new tenants are unusable. Own block (testng-is7nokm.xml) because
  the tenant-sharing config differs from the default-key-manager block.

  Scenario: A tenant provisioned without tenant-sharing has no key manager and key generation is refused
    Given The system is ready
    When I synchronize a new tenant "orphantenant.com" with admin password "Admin@123" via the tenant-sharing notify endpoint
    Then The response status code should be 200
    And the key manager list for admin "admin@orphantenant.com" password "Admin@123" has 0 entries
    When I attempt application key generation as admin "admin@orphantenant.com" password "Admin@123"
    Then The response status code should be 400
    And The response should contain "Key Manager not Registered"
