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
| `oracle_23c` | `oracle` | Oracle Free 23 in Testgrid, using native BOOLEAN schema scripts |
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

## Registry activity timestamp precision

The TestNG block parameter `registryLogMillisecondPrecision` is boolean and defaults to `false`.
On MySQL, opting in changes the block's shared-database `REG_LOG.REG_LOGGED_TIME` to
`TIMESTAMP(3) NOT NULL` after schema seeding and before APIM/CP boots. Provisioning reads
`INFORMATION_SCHEMA.COLUMNS` back and requires `DATETIME_PRECISION = 3`. Other database types
log that the parameter does not apply and retain their schema.

The shipped MySQL script uses second-precision `TIMESTAMP`, while the registry indexer advances
an activity timestamp cursor and queries with a strict `>` lower bound. Separate flushes can store
different caller timestamps in the same second and exclude a later activity. Millisecond precision
preserves those distinct timestamps; it does not prevent independently reordered producer events.

The parameter is enabled only on the regular and email-username DevPortal blocks in both V2 suites,
which run `DevPortalUnifiedSearchRunner` and assert registry indexing. It applies to hosted MySQL
profiles and the distributed block-owned MySQL database. All other blocks use the shipped schema.

## Secondary-store hybrid role key

The TestNG block parameter `hybridUserRoleKeyIncludesDomain` is boolean and defaults to `false`.
When enabled, Db2 hosted blocks replace the shipped three-column unique key on
`UM_HYBRID_USER_ROLE` with a four-column key that includes `UM_DOMAIN_ID`, then verify the key in
the Db2 catalog before APIM boots. Other database types already use the four-column key and are
left unchanged. The parameter is enabled only on all-in-one blocks that initialize the secondary user store;
the distributed suite omits it, as declared in `docs/devs/suite-alignment.ignore`.

## Suite checks

From this directory, run the Python-only checks:

```bash
python3 docs/devs/render_coverage_tree.py
python3 docs/devs/suite_alignment.py
```

The coverage-tree generator validates scenario tags and updates `docs/devs/coverage-tree.md`; the main CI workflow
also checks that the generated tree is committed fresh. The standalone suite validator writes nothing and compares
`testng-v2.xml` with `testng-v2_distributed.xml` by block, including runner multiplicity/order, scheduling and shared
local parameters. Adding a runner to one suite requires adding it to the matching block in the other, or declaring
the omission with a reason in [`docs/devs/suite-alignment.ignore`](docs/devs/suite-alignment.ignore). Undeclared
differences, unsupported execution selectors and stale declarations fail validation with file/line annotations.
Both the main and Testgrid build jobs run the validator before building images.

## JaCoCo coverage

JaCoCo collection is opt-in with `-Dapim.coverage=true`. In the all-in-one topology, the framework attempts a
dump for each TestNG block and, when dumps are available, merges them into a suite-level report. Coverage
collection is not enabled for the distributed topology. See the
[coverage architecture and local verification guide](docs/devs/v2-coverage-architecture.md) for setup, report
locations, and CI details.
