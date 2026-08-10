@setup
Feature: Setup comment threads for the plane-parity suite

  Builds the shared fixture the comment plane-parity scenarios read and then mutate. Three published APIs, so a
  root-comment COUNT on one plane is never disturbed by the other plane's fixture (the comment list of an API is
  plane-agnostic — a devportal list returns publisher-entered comments too, so one API cannot host both threads
  and still support an exact "count == 2"):

    * ptApiId<suffix> — the PUBLISHER-plane thread: ptRoot1<suffix> + ptRoot2<suffix>, three replies on ptRoot1<suffix>, one reply on ptRoot2<suffix>
    * dtApiId<suffix> — the DEVPORTAL-plane thread: the same shape, entered through the devportal plane
    * mtApiId<suffix> — no comments; the moderation/authorisation scenarios create their own there, so they stay
      self-contained and cannot perturb the counted threads above

  Each thread's shape mirrors the legacy fixture: root1 carries the three replies every list/pagination assertion
  is written against, and root2 carries a single reply that exists to be cascade-deleted from the OTHER plane.

  Comment ordering is load-bearing for the pagination assertions and is safe here: createdTime is stored with
  millisecond precision (observed: "2026-08-05 01:58:45.505"), so the sequential adds below get strictly
  increasing timestamps and the server's ordering is deterministic — root comments come back newest-first,
  replies oldest-first. This is why the legacy port needs no delay between adds even though the legacy test slept
  one second between each.

  Asserts nothing about product behaviour; the status checks are fail-fast gates so a fixture failure surfaces
  here rather than as a "No value found in context" cascade later. Resources are registered for the runner's
  AfterClass sweep (NOT per-scenario cleanup — the consumer features run off this fixture); comments are
  sub-resources of the API and cascade away with it.

  Scenario Outline: Publish three APIs and build a publisher-plane and a devportal-plane comment thread
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # --- The PUBLISHER-plane thread.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "ptApiId<suffix>" and deployed it
    And I publish the "apis" resource with id "ptApiId<suffix>"
    Then The lifecycle status of API "ptApiId<suffix>" should be "Published"
    When I add a "publisher" comment "This is root comment 1" with category "general" to API "ptApiId<suffix>" as "ptRoot1<suffix>"
    Then The response status code should be 201
    When I add a "publisher" comment "This is root comment 2" with category "general" to API "ptApiId<suffix>" as "ptRoot2<suffix>"
    Then The response status code should be 201
    When I add a "publisher" reply "This is a reply 1" to comment "ptRoot1<suffix>" of API "ptApiId<suffix>" as "ptReply1<suffix>"
    Then The response status code should be 201
    When I add a "publisher" reply "This is a reply 2" to comment "ptRoot1<suffix>" of API "ptApiId<suffix>" as "ptReply2<suffix>"
    Then The response status code should be 201
    When I add a "publisher" reply "This is a reply 3" to comment "ptRoot1<suffix>" of API "ptApiId<suffix>" as "ptReply3<suffix>"
    Then The response status code should be 201
    When I add a "publisher" reply "This is a reply" to comment "ptRoot2<suffix>" of API "ptApiId<suffix>" as "ptRoot2Reply<suffix>"
    Then The response status code should be 201

    # --- The DEVPORTAL-plane thread, same shape.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "dtApiId<suffix>" and deployed it
    And I publish the "apis" resource with id "dtApiId<suffix>"
    Then The lifecycle status of API "dtApiId<suffix>" should be "Published"
    When I add a "devportal" comment "This is root comment 1" with category "general" to API "dtApiId<suffix>" as "dtRoot1<suffix>"
    Then The response status code should be 201
    When I add a "devportal" comment "This is root comment 2" with category "general" to API "dtApiId<suffix>" as "dtRoot2<suffix>"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply 1" to comment "dtRoot1<suffix>" of API "dtApiId<suffix>" as "dtReply1<suffix>"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply 2" to comment "dtRoot1<suffix>" of API "dtApiId<suffix>" as "dtReply2<suffix>"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply 3" to comment "dtRoot1<suffix>" of API "dtApiId<suffix>" as "dtReply3<suffix>"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply" to comment "dtRoot2<suffix>" of API "dtApiId<suffix>" as "dtRoot2Reply<suffix>"
    Then The response status code should be 201

    # --- The moderation API: published, deliberately comment-free.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "mtApiId<suffix>" and deployed it
    And I publish the "apis" resource with id "mtApiId<suffix>"
    Then The lifecycle status of API "mtApiId<suffix>" should be "Published"

    Examples:
      | actor             | suffix       |
      | admin             |              |
      | admin@tenant1.com | @tenant1.com |
