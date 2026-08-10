@setup
Feature: Setup approval workflow executors

  Prepares the approval-workflow block by flipping the product's default Simple (auto-approve) workflow
  executors to the Approval variants: writes an alternate workflow-extensions.xml into the governance registry
  via the ResourceAdminService SOAP admin service (the only interface for that registry resource). The flip is
  picked up live (no restart). The registry resource is tenant-scoped, so the setup writes it as each tenant's
  admin; this block runs thread-count=1 and the runner's AfterClass restores both original contents. Asserts
  nothing about workflow behaviour (that is the scenarios'
  job). Each scenario that needs a published API or an approved application creates its own inline, because
  every scenario in this block is cleanup and the per-scenario cleanup hook sweeps all registered resources —
  so a runner-scoped shared fixture could not survive across the scenarios.

  Scenario Outline: Enable the approval workflow executors as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions.xml"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
