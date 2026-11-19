# Hosted MSSQL profile

The all-in-one `BlockLifecycleListener` accepts `-Dapim.test.db.profile=mssql`. Start the SQL Server instance
before Maven; the framework owns only the block databases. It creates separate APIM and Shared databases with
the case-sensitive `Latin1_General_CS_AS` collation, loads the matching schema scripts from the supplied APIM
distribution ZIP, enables `READ_COMMITTED_SNAPSHOT` on both databases, and drops the databases at block teardown.
Row-versioned reads prevent readers from waiting on concurrent writers at the default `READ COMMITTED` isolation
level while control-plane requests and background tasks update API metadata. The distributed suite does not consume
this profile.

Copy `tests-integration/cucumber-tests/src/test/resources/artifacts/configFiles/basic/database-mssql.env.example`
to a private file and replace its credentials and endpoints. The provisioning account needs permission to create
and drop databases and to create/map database users. The runtime login must already exist on SQL Server; the
framework grants it ownership of each block database. The JDBC provisioning endpoint must be reachable from the
test JVM, and the runtime endpoint must be reachable from the APIM containers. If SQL Server runs on a separate
Ubuntu VM, configure those endpoints for the two respective network paths rather than copying the example's
loopback and `host.docker.internal` values unchanged.

For example, to run the all-in-one suite with a private SQL Server profile file:

```sh
mvn test -pl tests-integration/cucumber-tests -am \
  -Dsurefire.suite.xml=testng-v2.xml \
  -Dapim.test.db.profile=mssql \
  -Dapim.test.db.envFile=/private/path/database.env \
  -Dapim.product.zip.path=/path/to/wso2am.zip
```

## Local verification on Apple Silicon

The SQL Server container used for local verification was run in an amd64 Ubuntu environment rather than directly
under ARM Colima. GitHub's `ubuntu-latest` Testgrid jobs are x64, so they run the SQL Server 2022 container without
that local architecture workaround. When the database and test JVM run on different hosts, ensure both the
provisioning and APIM runtime JDBC URLs can reach the SQL Server host.

The manually triggered [Integration V2 Testgrid workflow](../../../../../.github/workflows/integration-v2-testgrid.yml)
provisions an ephemeral SQL Server 2022 Developer Edition container when `mssql` is selected, creates a runtime
login, writes a runner-private profile file, and removes the container after the test job. Local runs must start
and stop their own database server.
