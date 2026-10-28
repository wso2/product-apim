# Evidence, attribution, running a block, and pitfalls

## Attribution

- Container lines: `<HH:MM:SS.mmm> [docker-java-stream-<id>] INFO DynamicApimContainer [default] - [<label>] : [<server
  ts UTC>] LEVEL - Class message`. The bracketed `[<label>]` is the block's `blockLabel` parameter.
- Test-thread lines carry `[TestNG-test=IntegrationV2-<TestName>-<n>]`. Cucumber's pretty output (Scenario/steps,
  failure stack traces) has **no** block prefix and is printed later than the Scenario header, interleaved with other
  concurrently running blocks — a failure can appear thousands of lines after its Scenario line.
- External IS lines carry no block label: they are tied to a `docker-java-stream-<id>` announced by
  `Initializing DynamicISContainer (... block='<label>')`. The splitter maps them best-effort; confirm by content
  (tenant names, the block's resource names).
- Attribute by resource identity, not by time alone: `${UNIQUE:...}` suffixes (`_5eb6180b_13`), API contexts,
  application names, tenant domain, actor. Timestamps narrow the window; identity proves the owner.
- Clocks: in CI logs the GitHub prefix and test-thread times are UTC; the server's `[yyyy-mm-dd hh:mm:ss,SSS]` is UTC.
  On a local machine the test-thread time is local and the server time is UTC — state which clock a quoted time uses.

## Reading the product

- There is no product source here; bytecode is the source of truth. `javap -c -p -l -cp <jar> <fully.qualified.Class>`
  gives instructions and the line-number table — match stack-trace line numbers to branches and quote them.
- OSGi bundles are under the server's `repository/components/plugins/`; REST APIs are webapps
  (`repository/deployment/server/webapps/api#am#publisher.war` etc., classes under `WEB-INF/classes`). Extract them from
  the image that ran (`docker create` a stopped container, `docker cp`, then remove it).
- Binary-inspection traps — each produced a silent false zero before: `grep` on a jar/class needs `-a`; `strings`
  fails on `.class`; `unzip` is absent inside the APIM image. Always run a positive control (search for a string you
  know is present) before trusting "not found".
- Configuration: the distribution's `deployment.toml` defaults and `conf/` templates explain overlay effects. TOML
  arrays in an overlay replace the default array (see defect C4).

## Proving an assertion (mutation soundness)

An assertion that cannot fail guards nothing. To prove a strengthened or new assertion:
- Mutate the **asserted value** in the feature (expected status, code, field value) to a value that is unreachable at
  every instant of the scenario — including warm-up responses (a 404 during gateway warm-up can make a "wrong" 404
  pass). Never mutate the request so that it becomes routable to something else.
- Grade on the assertion **message**, not on the failure count: the run must fail at the mutated step with the
  mutated value in the message.
- Restore the original and re-run green. Report both runs.

## Running a block

- Build a focused suite: the `<suite>` element and `<listeners>` of the suite file that ran (`testng-v2.xml` for
  all-in-one or `testng-v2_distributed.xml` for distributed) plus the block's `<test>` element copied verbatim, in
  `src/test/resources/zz-<label>.xml`; delete it afterwards. Copy the whole element — every runner, `thread-count`,
  order, parameters. Removing runners "to save time" removes exactly the interactions (parallel lanes
  on one server, shared fixtures, cleanup, ordering, global config) the audit exists to catch.
- From `all-in-one-apim/modules/integration-v2`: `mvn -o -B test -pl tests-integration/cucumber-tests -am
  -Dsurefire.suite.xml=zz-<label>.xml` (`-am` is mandatory; add `-Dplaywright.sso=true` for browser blocks). On macOS
  with Colima export, in the same command: `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
  TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock TESTCONTAINERS_HOST_OVERRIDE=localhost
  TESTCONTAINERS_RYUK_DISABLED=true`, and run under `caffeinate -dims`.
- `mvn test` does **not** rebuild the prebuilt images (`node-app-server`, the Solace shim) — only `mvn install`'s
  pre-integration-test execs do. If the change touches `tests-common/testcontainers/src/main/resources/nodeapps`,
  rebuild the node image from that directory first (`docker build -t node-app-server:latest <nodeapps>`), or build a
  private tag and pass `-Dnode.docker.image.name=<tag>` when others share the host.
- Never run two Maven builds in the same worktree at once (they clobber `target/` → bogus `NoClassDefFoundError`).
  Use one worktree per concurrent run.
- Runners read features from `src/` at run time: editing a feature mid-run changes the live run.

## Pitfalls that produced wrong conclusions before

- **ANSI stripping**: `tr -d '\033'` followed by a loose `sed 's/\[[0-9;]*m//g'` also deletes `[m` from text such as
  `[mt-sso]`. Strip with an exact pattern: `sed -E 's/\x1b\[[0-9;]*m//g'`.
- **Exit codes**: capture `rc=$?` on its own statement right after the command; `echo "$(date) rc=$?"` reports
  `date`'s status.
- **All scenarios fail on `baseUrl`**: the container never booted; discard the run.
- **Host sleep**: a lone `NoHttpResponseException` with a gap in container timestamps means the machine slept and the
  VM froze; discard the run and re-run under `caffeinate`.
- **Something else on 9443**: a local APIM on the host's 9443 silently answers probes meant for a container; always use
  the container's mapped port (`lsof -iTCP:9443 -sTCP:LISTEN` identifies it).
- **Exact match vs prefix**: when checking that a step/route/text exists, match on the distinguishing part; shared
  prefixes produce false "used" or "found" results.
- **`should contain`** in features is a substring match; a code checked that way can match unrelated text.
- **A green retry hides a race**: `Transient 900967 ... retrying`, `self-heal:` and repeated first-attempt 401/404
  lines in a passing block are E-class signals, not noise.
- **A catalogue entry can be stale**: re-verify a known product issue's signature on the build that ran.
