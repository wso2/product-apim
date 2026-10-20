# Hosted Db2 profile

The all-in-one `BlockLifecycleListener` accepts `-Dapim.test.db.profile=db2`. Start the database server
manually before running Maven. The framework creates an APIM database and a Shared database per block,
loads their schemas from the supplied product ZIP, installs the JCC driver in APIM, and drops the databases
after the block. Distributed suites do not consume this profile.

Copy `tests-integration/cucumber-tests/src/test/resources/artifacts/configFiles/basic/database-db2.env.example`
to a private file, replace its credentials and endpoints, and restrict the file to its owner (`chmod 600`).
The provisioning container must already run a Db2 instance. `database.provision.container` selects that
container for the Db2 CLP create/drop commands; the JDBC URLs select the endpoints used for seeding and
APIM access. The framework does not start or remove the database server.

```sh
mvn test -pl tests-integration/cucumber-tests -am \
  -Dsurefire.suite.xml=testng-v2.xml \
  -Dapim.test.db.profile=db2 \
  -Dapim.test.db.envFile=/private/path/database.env \
  -Dapim.product.zip.path=/path/to/wso2am.zip
```

The scripts are read from `dbscripts/apimgt/db2.sql` and `dbscripts/db2.sql` inside that ZIP. Their slash
terminators follow Db2 CLP's `-td/` mode: semicolons inside triggers remain part of the statement, and
slashes inside SQL literals or comments do not end statements. The current product APIM script omits
the unique key required by its foreign key to `AM_APPLICATION.UUID`; seeding adds that unique constraint
when the table definition does not already declare it.

## Local verification on Apple Silicon with Colima

The tested IBM Community Edition image is Db2 12.1.5.0:

```text
icr.io/db2_community/db2@sha256:2de8151713c261843868c5c3411b57be6ae79d99d70a5b3022337836776bfda6
```

It is an amd64 image. On an ARM Colima VM, both QEMU 7.0.0 and QEMU 10.2.3 user-mode emulation produced
intermittent `CREATE DATABASE` failures naming different `SYSIBMADM` modules. The failure also occurred
during the image's bootstrap-database creation, before the framework connected. QEMU 7 additionally
produced `initgroups: EPERM` and `Failed to read FVP pid` diagnostics; upgrading the emulator removed
those messages but did not resolve database creation. The identical image successfully created its
bootstrap database in a separate x86_64 Ubuntu VM. Use an amd64 Linux environment when checking
these database-engine failures. GitHub's native amd64 Ubuntu runners do not require emulation.

To keep APIM running natively on Apple Silicon, run only Db2 in a separate x86 VM. Colima needs QEMU
and its x86 guest agent installed (`brew install qemu lima-additional-guestagents`):

```sh
colima start db2-x86 --activate=false --arch x86_64 --vm-type qemu \
  --cpu 2 --memory 4 --disk 20 --mount none
```

Use `docker --context colima-db2-x86` to start the database container and publish its port to the Mac.
For example, publishing `50002:50000` makes the provisioning JDBC endpoint
`jdbc:db2://127.0.0.1:50002/APIMBOOT`. In the private profile file, set the runtime JDBC template to
the Mac host address reachable from the default Colima VM, rather than the database VM's loopback.
Resolve that address with `colima --profile default ssh -- getent hosts host.lima.internal`.

Run the IBM container with `--platform linux/amd64 --privileged=true`, and wait for
`/database/config/.shared-data/setup_complete` and a successful connection to its bootstrap database
before starting Maven. A fresh container does not require a manual `db2updv121` call.

For this two-VM arrangement, select the Db2 VM for the framework's Docker CLI provisioning commands
and the default VM socket for Testcontainers/APIM. Use an explicit CLI context: Docker 27.3.1 used
`DOCKER_HOST` even when `DOCKER_CONTEXT` was also exported in this verification environment. A
temporary wrapper keeps the two endpoints separate without changing framework code:

```sh
db2_cli_dir="$(mktemp -d)"
cat > "$db2_cli_dir/docker" <<'SH'
#!/bin/sh
exec /opt/homebrew/bin/docker --context colima-db2-x86 "$@"
SH
chmod 700 "$db2_cli_dir/docker"
export PATH="$db2_cli_dir:$PATH"
export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1
```

The wrapper path assumes Homebrew's Apple Silicon installation prefix. Keep it scoped to the
verification shell. On native amd64 Linux, both APIM and Db2 can use the same Docker daemon, so this
wrapper and the additional VM are unnecessary.

The APIM configuration follows the [WSO2 Db2 setup guide](https://apim.docs.wso2.com/en/latest/install-and-setup/setup/setting-up-databases/changing-default-databases/changing-to-ibm-db2/):
`type=db2`, the `com.ibm.db2.jcc.DB2Driver` driver, and the validation query
`SELECT 1 FROM SYSIBM.SYSDUMMY1`. Keep credentials in the private environment file.
