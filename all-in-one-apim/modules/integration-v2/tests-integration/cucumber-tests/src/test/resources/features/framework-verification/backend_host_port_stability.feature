@framework
Feature: Framework Verification - the shared backend's published host port survives block network churn

  Pins the invariant every host-facing backend URL depends on: the address must not change for the
  container's whole life.

  The shared node backend is the only container re-homed after start - each block attaches it to that
  block's private network and detaches it at teardown. Docker reallocates an ephemeral published port on
  every one of those connects and disconnects, while testcontainers answers getMappedPort from the inspect
  snapshot taken at start. Left unpinned, every host-read backend URL can point at a port nothing is
  listening on and fail with a bare "connection refused" that names neither docker nor network sharing.

  Three cycles, not one: the first connect is the only one that a "re-read the port after attaching" fix
  would cover, so a single cycle would pass against an insufficient fix.

  Both the live binding and the cached value are asserted each time. The live binding is the one that
  drifts; the cached value is what the tests actually build URLs from. Checking only the cached one would
  compare a snapshot with itself and pass while the real binding moved.

  Scenario: The backend's published host port and reachability are unchanged by attach and detach cycles
    Then The shared backend's published host port for container port 3005 survives 3 network attach and detach cycles
