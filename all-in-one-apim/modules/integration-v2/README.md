# Integration Test Framework V2

This module contains the V2 integration-test framework and product suites.

## Database profiles

The all-in-one V2 suite uses its default H2 databases unless a hosted database profile is explicitly selected.
Hosted profiles are applied by `BlockLifecycleListener` in `testng-v2.xml`; they do not change the distributed
suite. For each all-in-one block, the listener creates separate APIM and Shared databases (or Oracle schemas),
loads the matching schema scripts from the built APIM distribution, and removes the block's databases or schemas
at teardown. The database server itself is managed outside the block lifecycle.

Supported hosted profiles are:

| Profile | APIM database type | Notes |
|---|---|---|
| `mysql` | `mysql` | MySQL 8.4 in Testgrid |
| `postgresql` | `postgre` | PostgreSQL 16 in Testgrid |
| `mssql` | `mssql` | SQL Server 2022 in Testgrid |
| `oracle` | `oracle` | Oracle Free in Testgrid |
| `oracle-rac` | `oracle` | Uses an externally managed Oracle RAC SCAN/service endpoint |
| `db2` | `db2` | Db2 LUW; the Testgrid profile provisions Db2 Community Edition |

For local all-in-one runs, pass `-Dapim.test.db.profile=<profile>` and
`-Dapim.test.db.envFile=<path-to-private-properties-file>`. Example configuration files are in
[`tests-integration/cucumber-tests/src/test/resources/artifacts/configFiles/basic`](tests-integration/cucumber-tests/src/test/resources/artifacts/configFiles/basic).
Keep real credentials in a private, untracked file. The manually triggered
[Integration V2 Testgrid workflow](../../../.github/workflows/integration-v2-testgrid.yml) has an H2 default and
lets you select one of the hosted profiles; its build and test jobs are separate so the test job can be retried
without rebuilding the product images.

See the [hosted MSSQL guide](docs/devs/hosted-mssql.md) for SQL Server setup and local amd64 Ubuntu notes, the
[hosted Db2 guide](docs/devs/hosted-db2.md) for Db2 setup and local Colima notes, and the
[Oracle RAC guide](docs/devs/hosted-oracle-rac.md) for external RAC configuration and its verification limits.

## JaCoCo coverage

JaCoCo collection is opt-in with `-Dapim.coverage=true`. In the all-in-one topology, the framework captures a
dump for each TestNG block and merges the dumps into a suite-level report. Coverage collection is not enabled
for the distributed topology. See the [coverage architecture and local verification guide](docs/devs/v2-coverage-architecture.md)
for setup, report locations, and CI details.
