@cleanup
Feature: Publisher Network Access Control - tenant-configuration policy source

  The outbound host-validation policy can be configured per tenant in tenant-conf.json
  (NetworkSecurityAccessControl), independently of the platform deployment.toml source. This runs in a
  container with NO platform policy, so the gate is driven purely by the tenant policy. A tenant that
  configures a deny policy has its references blocked, while another tenant with no policy does not block the
  same reference - proving the tenant-conf source both enforces and is tenant-scoped. The original tenant
  configuration is restored at the end; the runner must stay single-scenario so the mutation cannot bleed into
  a sibling. Runs in the network-access-control-tenant-conf container (needs the node fixtures backend on
  nodebackend:3021).

  @cap:publisher @feat:network-access-control @rule:tenant-conf-policy @type:negative @dep:admin
  Scenario: A tenant-configuration deny policy blocks a reference for that tenant only
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    And The system is ready and I have valid publisher access tokens as "publisherUser@tenant1.com"
    And The system is ready and I have valid publisher access tokens as "publisherUser"
    And I act as "admin@tenant1.com"
    And I capture the tenant configuration as "nacTenantConfOriginal"
    And I capture the tenant configuration as "nacTenantConf"
    And I set the JSON object field "NetworkSecurityAccessControl" from file "artifacts/payloads/networkAccessControl/nac_tenant_deny_nodebackend.json" in the payload "nacTenantConf"
    And I update the tenant configuration from "nacTenantConf"
    Then The response status code should be 200
    When I act as "publisherUser@tenant1.com"
    And I validate the openapi definition from file "artifacts/payloads/networkAccessControl/oas30_nodebackend_ref.json"
    Then The response status code should be 400
    And The response should contain "not trusted"
    And The response should contain "definition contains a URL that is not trusted"
    When I act as "publisherUser"
    And I validate the openapi definition from file "artifacts/payloads/networkAccessControl/oas30_nodebackend_ref.json"
    Then The response status code should be 200
    And The response should not contain "not trusted"
    When I act as "admin@tenant1.com"
    And I update the tenant configuration from "nacTenantConfOriginal"
    Then The response status code should be 200
