@cap:admin @feat:external-key-manager @rule:tenant-sharing @type:negative @cleanup
Feature: External Key Manager Tenant Sharing Disabled

  With [apim.key_manager] skip_create_resident_key_manager=true but tenant-sharing OFF (no [[apim.tenant_sharing]]),
  a tenant provisioned through the notify endpoint is created with NO key manager - neither a Resident KM (skipped)
  nor an auto-configured WSO2-IS-7 KM (tenant-sharing off). Application key generation is then refused with
  901403 "Key Manager not Registered". This pins the documented dependency: skipping the Resident KM REQUIRES
  tenant-sharing to supply a WSO2-IS-7 KM, else new tenants are unusable. Own block (Is7TenantSharingOff) because
  the tenant-sharing config differs from the default-key-manager block. The synced tenant's admin becomes a
  RUNTIME actor, so the standard steps and cleanup apply to everything it creates.

  Scenario: A tenant provisioned without tenant-sharing has no key manager and key generation is refused
    Given The system is ready
    And I have valid access tokens as "admin"
    When I synchronize a new tenant "orphantenant.com" with admin password "Admin@123" via the tenant-sharing notify endpoint
    Then The response status code should be 200
    When I register the runtime tenant admin "admin@orphantenant.com" with password "Admin@123" as an actor
    And I have valid access tokens as "admin@orphantenant.com"
    Then the key manager list for the acting actor has 0 entries
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app_oauth.json" in context as "tsoAppPayload"
    And I create an application with payload "tsoAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "tsoKeygenPayload"
    """
    {"keyType": "PRODUCTION", "keyManager": "Resident Key Manager", "grantTypesToBeSupported": ["client_credentials"], "validityTime": 3600}
    """
    And I generate client credentials for application id "createdAppId" with payload "tsoKeygenPayload"
    Then The response status code should be 400
    And The response should contain "Key Manager not Registered"
