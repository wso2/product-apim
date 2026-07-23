@cap:admin @feat:external-key-manager @rule:tenant-sharing @type:regression
Feature: External Key Manager Tenant Sharing Auto-Configuration

  With [[apim.tenant_sharing]] type=WSO2-IS-7 (auto_configure_key_manager=true) and [apim.key_manager]
  skip_create_resident_key_manager=true, a tenant provisioned through the tenant-sharing notify endpoint
  (/internal/data/v1/notify) is auto-configured with a WSO2-IS-7 key manager INSTEAD of the Resident Key
  Manager - the documented default-key-manager-for-new-tenants path. skip_create_resident_key_manager also
  suppresses the Resident KM for the super tenant. Runs in its own block (Is7DefaultKeyManager) because that
  flag is a server-wide behaviour change. The synced tenant's admin becomes a RUNTIME actor, so the standard
  auth composites and admin REST steps apply to it.

  Scenario: A tenant synchronized via tenant-sharing is auto-configured with a WSO2-IS-7 key manager
    Given The system is ready
    And I have valid access tokens as "admin"
    When I synchronize a new tenant "sharedtenant.com" with admin password "Admin@123" via the tenant-sharing notify endpoint
    Then The response status code should be 200
    When I register the runtime tenant admin "admin@sharedtenant.com" with password "Admin@123" as an actor
    And I have valid access tokens as "admin@sharedtenant.com"
    Then the key manager list for the acting actor has 1 entries
    And the key manager list includes a "WSO2-IS-7" key manager

  Scenario: The resident key manager is skipped for the super tenant
    Given The system is ready
    And I have valid access tokens as "admin"
    Then the key manager list for the acting actor has 0 entries
