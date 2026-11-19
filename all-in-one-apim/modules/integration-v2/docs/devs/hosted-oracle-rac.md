# Oracle RAC for Integration v2

The all-in-one `BlockLifecycleListener` supports `-Dapim.test.db.profile=oracle-rac`. It creates a pair of
block-specific Oracle schema users in the configured service, loads APIM's RAC-specific schema scripts from the
product distribution ZIP (`dbscripts/apimgt/oracle_rac.sql` and `dbscripts/oracle_rac.sql`), and drops both users
when the block ends. The database server itself is externally managed.

The connection URL must identify an Oracle RAC SCAN listener and service, normally in Easy Connect form:

```text
jdbc:oracle:thin:@//<SCAN-host>:1521/<service-name>
```

Use the same RAC URL for provisioning and APIM runtime connections. It must be reachable from both the test host
and the APIM containers. The provisioning account needs permission to create/drop users, grant the schema user's
object privileges, and grant `DBA`, as required by the APIM schema setup. Keep passwords in an untracked env file.
The example is at
`tests-integration/cucumber-tests/src/test/resources/artifacts/configFiles/basic/database-oracle-rac.env.example`.

The Testgrid workflow adds an `oracle-rac` database choice. Configure these repository or environment secrets:

* `ORACLE_RAC_JDBC_URL`: RAC SCAN and service JDBC URL.
* `ORACLE_RAC_PROVISION_USERNAME`: schema provisioning account.
* `ORACLE_RAC_PROVISION_PASSWORD`: its password.

The Testgrid runner uses the supplied endpoint and does not launch or own the RAC server. Make sure the selected
GitHub runner has network access to the RAC listener. The `oracle_23c` choice provisions Oracle Free 23 and applies
`oracle_23c.sql` and `apimgt/oracle_23c.sql`, which define Oracle 23 native `BOOLEAN` columns. The `oracle-rac`
profile remains separate and always uses its RAC-specific `oracle_rac.sql` scripts.

The local RAC-profile smoke test can use an Oracle Free container as a practical way to exercise the framework's
per-block schema creation, RAC-specific product scripts, APIM startup, and cleanup. It does not verify RAC SCAN
load balancing, failover, or multi-node behavior. A real RAC endpoint is required to verify those properties.

This follows WSO2's [Oracle RAC database setup guide](https://apim.docs.wso2.com/en/latest/install-and-setup/setup/setting-up-databases/changing-default-databases/changing-to-oracle-rac/).
