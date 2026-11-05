@framework
Feature: Framework Verification - the shared backend's published host port survives block network churn

  Pins the invariants every host-facing backend URL depends on: the address must not change, and the
  service must remain reachable while another block changes the shared container's network attachments.

  The shared node backend is the only container re-homed after start - each block attaches it to that
  block's private network and detaches it at teardown. Docker reallocates an ephemeral published port on
  every one of those connects and disconnects, while testcontainers answers getMappedPort from the inspect
  snapshot taken at start. Left unpinned, every host-read backend URL can point at a port nothing is
  listening on and fail with a bare "connection refused" that names neither docker nor network sharing.

  Three cycles exercise repeated attach/detach churn. A continuous HTTP probe hits the BPMN mock's
  request-inspection endpoint during those changes, so a stable port number cannot hide a short-lived
  forwarding outage.

  Both the live binding and the cached value are asserted each time. The live binding is the one that
  drifts; the cached value is what the tests actually build URLs from. Checking only the cached one would
  compare a snapshot with itself and pass while the real binding moved.

  Scenario: The backend's published host port and reachability are unchanged by attach and detach cycles
    Then The shared backend's published host port for container port 3004 survives 3 network attach and detach cycles
