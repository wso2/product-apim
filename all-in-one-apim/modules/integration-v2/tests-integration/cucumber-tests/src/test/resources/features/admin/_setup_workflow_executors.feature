@setup
Feature: Setup approval workflow executors

  Prepares the approval-workflow block by flipping the product's default Simple (auto-approve) workflow
  executors to the Approval variants: writes an alternate workflow-extensions.xml into the governance registry
  via the ResourceAdminService SOAP admin service (the only interface for that registry resource). The flip is
  picked up live (no restart) and is server-global, so this block runs thread-count=1 and the runner's
  AfterClass restores the original content. Asserts nothing about workflow behaviour (that is the scenarios'
  job). Each scenario that needs a published API or an approved application creates its own inline, because
  every scenario in this block is cleanup and the per-scenario cleanup hook sweeps all registered resources —
  so a runner-scoped shared fixture could not survive across the scenarios.

  Scenario: Enable the approval workflow executors
    Given The system is ready
    And I have valid access tokens as "admin"
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions.xml"
