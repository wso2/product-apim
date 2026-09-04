Feature: Admin Tenant Domain Validation

  Tenant-creation input validation on the TenantMgtAdminService admin service: a domain carrying a character
  outside the allowed set (lower-case letters, digits, '.', '-' and '_') must be REJECTED, not silently
  accepted. Ports the tenant-creation half of TenantDomainValidationTestCase. The gateway half - a mis-cased
  tenant domain in the invocation path is rejected and leaves the tenant's routing intact - is covered by the
  "mis-cased tenant domain" scenario in gateway/rest_invocation, which is where its assertions belong.

  The domain "Abc.com" is the legacy constant, mis-cased on purpose: the upper-case "A" is the illegal
  character. It is written literally rather than uniquely generated because a REJECTED create leaves no
  resource behind, so there is nothing for a parallel runner to collide with.

  The legacy test asserted this by catching an exception - which meant it also passed when the product
  accepted the invalid domain and threw nothing at all. This scenario pins the outcome positively instead:
  the exact fault status and the validation message naming the offending domain.

  @cap:admin @feat:tenants-orgs @type:negative @legacy:TenantDomainValidationTestCase
  Scenario: Creating a tenant whose domain contains an illegal character is rejected
    Given The system is ready
    And I act as "admin"
    When I attempt to create a tenant with domain "Abc.com" and admin password "password1"
    Then The response status code should be 500
    And The response should contain "Abc.com contains one or more illegal characters"
