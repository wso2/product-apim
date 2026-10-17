# Integration Test Framework V2

This module contains the V2 integration-test framework and product suites.

## JaCoCo coverage

JaCoCo collection is opt-in with `-Dapim.coverage=true`. In the all-in-one topology, the framework captures a
dump for each TestNG block and merges the dumps into a suite-level report. Coverage collection is not enabled
for the distributed topology. See the [coverage architecture and local verification guide](docs/devs/v2-coverage-architecture.md)
for setup, report locations, and CI details.
