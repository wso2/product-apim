# Secondary User Store as a Framework Facility — Architecture (AS BUILT)

Goal: turn the ad-hoc, boot-time, super-tenant-only secondary user store (from the G3 port) into a
**framework-provided facility** that any block can opt into, with secondary-store users as first-class **actors**
in **both tenants**, backed by a DB whose type **tracks the suite's primary DB** (H2 now; MySQL/etc. later).

**Status: Phases 0–2 BUILT and verified green** (block `IntegrationV2-SecondaryUserStore`, 7/7). The rest of this
doc is the design; the **As-built findings** section below records what the implementation empirically confirmed
or changed versus the plan. What ships:
`SecondaryUserStoreProvisioner` + `DynamicApimContainer#createSecondaryUserStoreH2Schema` +
`TenantUserProvisioner#addSecondaryUserStore/waitUntilStoreActive/addStoreUserAsActor` +
the `initSecondaryUserStore` block param, registered + seeded for BOTH tenants.

## As-built findings (verified against a live container)
1. **Runtime everything works** — fresh empty H2 schema from the product's own `dbscripts/h2.sql` (run in-container
   as `wso2carbon`) → `addUserStore` SOAP (hot-deploys async, ~10s) → poll-until-active. No seeded `.mv.db`, no
   boot XML, no 0666 hack. The seeded-binary fixture (`WSO2SEC_DB.mv.db` + `SECONDARY.xml`) is DELETED.
2. **Shared-DB tenant isolation CONFIRMED** — one H2 DB backs both tenants' `SECONDARY.COM` stores; a user seeded
   in `carbon.super` is invisible to `tenant1.com` (distinct `UM_TENANT_ID`). Asserted by the isolation scenario.
3. **Fresh store is EMPTY on registration** — `addUserStore` does NOT auto-create an admin. The `SECONDARY.COM/admin`
   seen in the manual console recipe came from copying a pre-seeded `WSO2SHARED_DB`, not from registration. Every
   store user is one the framework explicitly seeds. Asserted (`SECONDARY.COM/admin should not exist`).
4. **Store users ARE first-class actors** — `SECONDARY.COM/publisherUser1` and `SECONDARY.COM/subscriberUser1`
   authenticate (DCR + password grant) and drive the publisher / devportal planes in BOTH tenants → the full ×4.
   **Caveat (fixed):** the DCR client-name regex `^[\sa-zA-Z0-9._-]*$` rejects the store-domain `/`, so
   `BaseSteps#createDcrApplication` now sanitizes the DERIVED client name (the OAuth `owner` is untouched).
5. **A store user CANNOT be a true admin (hard limit).** `addUser` accepts the primary `admin` role on a
   secondary-store user (returns 2xx), but the resulting account then **fails DCR basic-auth with 401** and is
   unusable — while the `Internal/` hybrid-role users (publisher/subscriber) authenticate fine in the same run.
   So the primary `admin` role poisons a secondary-store account; **admin-plane coverage stays on primary-store
   admins.** No admin store user is seeded.

---

## The 4× actor matrix we are building toward
Today scenarios run ×2 over the tenant axis (super + tenant1). Target: also run over the **user-store axis**
(primary + secondary domain) → **×4**:

As built, the secondary axis uses two least-privilege archetypes mirroring the primary store (a store user cannot
be a true admin — see finding #5), so the ×4 is {publisher, subscriber} × {super, tenant1}:

| actor key | tenant | user store | role |
|---|---|---|---|
| `publisherUser` / `subscriberUser` | carbon.super | PRIMARY | creator+publisher / subscriber |
| `publisherUser@tenant1.com` / `subscriberUser@tenant1.com` | tenant1.com | PRIMARY | ″ |
| `SECONDARY.COM/publisherUser1` | carbon.super | SECONDARY | creator+publisher |
| `SECONDARY.COM/publisherUser1@tenant1.com` | tenant1.com | SECONDARY | creator+publisher |
| `SECONDARY.COM/subscriberUser1` | carbon.super | SECONDARY | subscriber |
| `SECONDARY.COM/subscriberUser1@tenant1.com` | tenant1.com | SECONDARY | subscriber |

The actor key deliberately carries the store domain so a feature's `Examples` table is self-documenting; it is
also the store-local username (`resolveActor` splits on `@`, which follows the key). The value: prove the product
behaves **identically regardless of which user store the acting principal lives in** — a real regression dimension
the suite previously didn't cover.

---

## Two things that are conflated today — separate them
The current design copies **one thing** (a seeded `.mv.db`) and deploys **another** (the `SECONDARY.xml` config).
The architecture must treat these as two independent concerns:

1. **Schema provisioning** — the usermgt `UM_*` tables must EXIST in the target DB before the store is used.
   (The G3 "Database error retrieving userID" was a *missing schema*, not a config problem.)
2. **Store-config registration** — telling Carbon "there is a `SECONDARY` domain backed by this JDBC source".

Keeping them separate is what makes the design DB-extensible and lets us drop the fragile seeded-binary + 0666
hack.

---

## Q2 — Can one DB file back multiple tenants' secondary stores? YES (recommended)
The WSO2 usermgt schema is **tenant-aware**: `UM_USER`, `UM_USER_ROLE`, `UM_ROLE`, `UM_USER_ATTRIBUTE` … all
carry a **`UM_TENANT_ID`** column, and the uniqueness key on a user is **(`UM_USER_NAME`, `UM_TENANT_ID`)**. A
JDBC user-store manager runs in its owning tenant's context and reads/writes with that tenant's id. Therefore:

- super-tenant's `SECONDARY` store (`UM_TENANT_ID = -1234`) and tenant1's `SECONDARY` store (`UM_TENANT_ID = 1`)
  can share **one physical DB** (one `.mv.db`, one usermgt schema) with **zero data collision** — the same
  username in each tenant is a different row.
- H2 embedded allows multiple connections from the **same JVM** (the Carbon process hosts both stores), so no
  file-lock conflict.

So we do **not** need a DB file per tenant. One shared secondary DB (H2 file / MySQL database) serves all
tenants. **Spike to confirm** the JDBC secondary store writes the owning tenant's `UM_TENANT_ID` (it should), and
that `UserNameUniqueAcrossTenants=false` is set.

(Recall Q on "multiple DBs in one H2 file": still no — one `.mv.db` = one H2 database. But we don't need multiple
DBs; we need multiple **tenants** in one usermgt schema, which `UM_TENANT_ID` already gives us. On MySQL the same
holds, and MySQL additionally *can* host a separate `CREATE DATABASE wso2sec` cleanly.)

---

## Q3 — Framework registration: all-runtime SOAP (hot-deploy CONFIRMED)
Runtime `UserStoreConfigAdminService.addUserStore` **does hot-deploy** (user-confirmed; re-probe when
implementing). So the design collapses to the clean path — **no hybrid, no boot XML, no seeded binary**:
at provisioning time, for each tenant, call `addUserStore` (SOAP) after the schema is created via **runtime DDL**.
The G3 "never hot-deploys" observation was the **missing-schema symptom**, not a deployer limit.

### What "runtime DDL" means (and the embedded-H2 nuance)
DDL = the SQL that CREATEs the usermgt tables (`UM_USER`, `UM_ROLE`, `UM_USER_ROLE`, `UM_USER_ATTRIBUTE`, …),
shipped by WSO2 as `dbscripts/<db>.sql`. The JDBC user store does NOT create its own tables — it expects them to
exist. "Runtime DDL" = create them **live at provision time** against a fresh empty DB the server creates itself,
instead of shipping a pre-baked `.mv.db`. Benefit: **no committed binary and no 0666 hack** (the file is written
by `wso2carbon`, writable by construction).

**Strict ordering (always):** create schema → `addUserStore` → seed users. The tables must exist before the
store's first read/write.

**H2 is single-PROCESS, not single-connection:** one JVM at a time holds the `.mv.db` file lock, but that JVM may
hold many connections. So Carbon can connect freely; the *external* test framework cannot open the file while
Carbon holds it → the DDL for embedded H2 must run **container-side**, not from the framework's JDBC.

The mechanism is the ONE DB-specific step (hence the strategy seam):
- **Embedded H2** — run the DDL inside the container, **as the `wso2carbon` user** (NOT root — a root-owned
  `.mv.db` reintroduces the exact 0666 read-only problem). Preferred: a **boot exec before Carbon starts**
  (`org.h2.tools.RunScript -url jdbc:h2:./…/WSO2SEC_DB -user wso2carbon -script dbscripts/h2.sql`) — nothing holds
  the file yet, RunScript creates it + tables + releases the lock, then Carbon starts and the runtime
  `addUserStore` opens the already-seeded, writable file. (A `docker exec` RunScript *after* start also works,
  since Carbon does not lock `WSO2SEC_DB` at boot — it's not a configured datasource until the store is
  registered — but boot-before-start avoids the timing window.) Alternative: the store URL's
  `;INIT=RUNSCRIPT FROM 'dbscripts/h2.sql'` (runs on first connect, in-JVM, no lock issue — but needs idempotency
  guarding so it doesn't re-run every connection).
- **Networked DB (MySQL/Postgres)** — no file lock; the framework connects over the network and runs
  `CREATE DATABASE wso2sec` + the DDL directly, then `addUserStore`. Same order, no in-container exec.

Implication for the framework: embedded-H2 schema creation is a new **"run this DDL as wso2carbon at boot"**
capability (a peer of `serverFilesToCopy`), replacing the copied seeded binary.

**Principle — the framework owns ZERO DDL content.** Always use the product's own **shipped, version-matched
scripts inside the container** (`<CARBON_HOME>/dbscripts/<db>.sql`) and the container's bundled tooling (WSO2
ships H2, so `org.h2.tools.RunScript` is already on the classpath). A framework-side copy of the schema would
drift from the product's real `UM_*` schema across versions — a correctness trap. The framework decides only
*when* / *as whom* the script runs; it never authors or stores DDL. (Build-time detail: pin which shipped
script/subset carries the usermgt `UM_*` DDL for a JDBC user store — running the full `h2.sql` is harmless but
running just the usermgt subset is cleaner.)

Everything downstream of schema creation (`addUserStore` SOAP, poll-until-active, actor seeding) is **identical
across DBs**.

**Probe when implementing:** time-to-active after `addUserStore` (drives a poll-until-ready, never a sleep), and
confirm a `SECONDARY/…` user authenticates for publisher/devportal/admin tokens transparently.

---

## Proposed component: `SecondaryUserStoreProvisioner` (framework)
A single provisioner, invoked by the block lifecycle when opted in, that per (tenant, DB-config) does:

1. **Ensure schema** — via a `SecondaryStoreDbStrategy` (below): create the secondary schema/DB and run the
   usermgt DDL if absent (idempotent).
2. **Register store config** — `addUserStore` (SOAP) for the tenant: domain `SECONDARY`, JDBC url/driver pointing
   at the provisioned schema, properties (`CaseInsensitiveUsername`, regexes, `UserNameUniqueAcrossTenants=false`).
   (Fallback: boot XML for super.)
3. **Wait until active** — poll (list user stores / `isExistingUser` on a probe), never sleep.
4. **Seed actors** — `addStoreUserAsActor` for the secondary least-priv users with **domain-qualified** names
   (`SECONDARY.COM/publisherUser1` → `Internal/creator,publisher`; `SECONDARY.COM/subscriberUser1` →
   `Internal/subscriber`) — mirroring the primary-domain archetypes. (A true-admin store actor was attempted and
   proven not achievable — finding #5 — so none is seeded.)

**Integration points (Q3):**
- **Super-tenant provisioning** (the block's default actor seeding, alongside `publisherUser`/`subscriberUser`/…)
  — call the provisioner for `carbon.super` when the block opts in.
- **`TenantUserProvisioner`** — call it for `tenant1.com` too.
- **Opt-in**: a new block parameter **`initSecondaryUserStore=true`** (peer of `initTenantUsers`, `initBackend`,
  `serverFilesToCopy`) parsed in `BlockLifecycleListener`. Only blocks that need it pay the cost.

## Actor model extension (Q1) — as built
- No change needed to `utils/Identity`: `resolveActor` already keys off `<key>@<tenant>`, and the store-qualified
  key (`SECONDARY.COM/publisherUser1`) resolves because the split is on `@` (which follows the key). The secondary
  actors are registered into the tenant bean by `addStoreUserAsActor` (key == store-local username; the auth layer
  appends `@<tenant>` to form the full login name).
- Auth composites resolve tokens for a secondary actor exactly like primary ones — the KM authenticates against the
  SECONDARY store transparently once the user exists there (publisher/devportal confirmed). The one code change was
  sanitizing the DERIVED DCR client name (finding #4).
- Scenarios opt into the ×4 matrix with a Scenario Outline over an actor column. Most scenarios stay ×2; the ×4 is
  used where "works across user stores" is the point.

## DB extensibility (Q4) — strategy pattern keyed on the suite's DB
Abstract the DB-specific bits behind `SecondaryStoreDbStrategy`, selected from the **suite's primary DB** (read
from `API_MANAGER_DATABASE_DRIVER`/`_URL` env, the same knobs a MySQL run would flip):

| Concern | H2 (implement now) | MySQL (future) | Postgres (future) |
|---|---|---|---|
| Secondary schema | separate `WSO2SEC_DB.mv.db` (or a `SCHEMA` in an existing writable file) | `CREATE DATABASE wso2sec` | `CREATE SCHEMA` / DB |
| DDL | `dbscripts/h2.sql` at runtime | `dbscripts/mysql.sql` | `dbscripts/postgresql.sql` |
| JDBC url/driver for the store | `jdbc:h2:./…/WSO2SEC_DB`, `org.h2.Driver` | `jdbc:mysql://…/wso2sec`, mysql driver | `jdbc:postgresql://…` |
| Create mechanism | server creates the file (runtime DDL) | issue `CREATE DATABASE` on the server | `CREATE DATABASE/SCHEMA` |

The `addUserStore` registration and the actor seeding are **DB-agnostic** — only url/driver/DDL vary. So "H2 run →
H2 secondary; MySQL run → MySQL secondary" falls out automatically because the strategy reads the same DB config
the suite already uses. Implement only the H2 strategy now; the interface reserves the seam for the rest.

---

## Phased path
- **Phase 0 — Spike:** DONE. Proved runtime-DDL schema (H2 in-container RunScript) + `addUserStore` + time-to-active
  + a secondary user getting a token.
- **Phase 1 — H2 facility:** DONE. `SecondaryUserStoreProvisioner` + `createSecondaryUserStoreH2Schema` +
  `initSecondaryUserStore` block param + the runtime `addUserStore`/poll-active/seed path. The seeded-`.mv.db` +
  boot XML fixture is deleted and the 0666 dependency is gone; the existing 2 scenarios run on the facility.
- **Phase 2 — ×4 actors:** DONE. Store + actors registered in both tenants; ×4 = {publisher, subscriber} × {super,
  tenant1} green (a true-admin store actor is not achievable — finding #5).
- **Phase 3 — multi-DB:** add MySQL/Postgres strategies when the suite gains non-H2 runs; the H2 schema creation is
  isolated in `SecondaryUserStoreProvisioner`/`DynamicApimContainer` so only that step changes. (The formal
  `SecondaryStoreDbStrategy` interface below is reserved, not yet extracted — a single H2 path ships.)

## Risks / open items — all RESOLVED in Phase 0–2 (see As-built findings)
1. ~~Hot-deploy of runtime `addUserStore`~~ **CONFIRMED** — hot-deploys async (~10s), poll-until-active.
2. ~~Shared-DB tenant isolation~~ **CONFIRMED** — one DB, `UM_TENANT_ID`-isolated; cross-tenant invisibility asserted.
3. ~~Store-active timing~~ **~10s** observed; `waitUntilStoreActive` polls (60s deadline), never sleeps.
4. ~~Token issuance for secondary users~~ **CONFIRMED for publisher/devportal** (password grant flows the
   domain-qualified username). Required fixing the DCR client-name sanitization (finding #4). **Admin token does
   NOT work** — a store user can't be a true admin (finding #5).
5. **Cleanup** — the whole container is discarded at block end, so seeded store users/roles never leak cross-run;
   per-scenario artifacts are swept by the `@cleanup` hook as their creating actor (incl. the store actors).

## Net
- One shared secondary DB per suite (tenant-isolated via `UM_TENANT_ID`) — not per-tenant files.
- Runtime SOAP registration + runtime DDL schema = no seeded binary, no 0666 hack, per-tenant, per-block.
- A DB-strategy seam so the secondary store's DB type always tracks the suite's DB type.
- Secondary-domain actors → the ×4 (2 tenants × 2 stores) coverage expansion.
- Ship H2 now; the MySQL/Postgres path is architected, not built.
