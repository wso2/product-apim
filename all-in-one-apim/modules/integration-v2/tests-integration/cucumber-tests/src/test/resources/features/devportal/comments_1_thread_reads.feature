Feature: API Comment Threads — Read Surface Across Both Planes

  Every READ of a comment thread, on both entry planes and across them. Comments are one capability with two
  entry points: the same thread is addressable through the publisher plane and the devportal plane, and each
  comment records which plane it was entered from in its entryPoint. This feature pins the exact shape of every
  read — the root list, a comment fetched with its replies, and the dedicated /replies collection — each in its
  full form and again under limit/offset, on the publisher plane, on the devportal plane, and cross-plane in both
  directions. Ports the read half of PublisherCommentTest and DevPortalCommentTest.

  What "exact" means here, and why each assertion is what it is:
    * count is the size of the RETURNED PAGE, pagination.total is the size of the whole collection. Both are
      asserted, since a page-size-limited read that still reports the true total is the property the legacy
      total-of-pagination tests were after (legacy guarded those behind a null check on total, so they often did
      not run at all; here they are unconditional).
    * Root comments come back NEWEST-FIRST and replies OLDEST-FIRST — an asymmetry the pagination assertions
      depend on, so it is asserted positionally rather than left implicit. offset 1 over two roots therefore
      returns root 1, and offset 1 over three replies returns replies 2 and 3.
    * Each list member is pinned by ID to the fixture comment it must be, not merely "one of" them: the legacy
      loop asserted only set membership, which passes even if the same comment appears twice.
    * A root's parentCommentId is asserted to be present-and-null; the API renders it explicitly as
      "parentCommentId":null, so a substring check would be ambiguous with the replies nested in the same payload.

  The three published APIs and both threads come from _setup_comment_threads, which sorts first in the runner
  (execution order is lexicographic by feature filename, not array order). Every scenario here is READ-ONLY, so
  they are order-independent among themselves; the destructive edit/delete scenarios live in
  comments_2_thread_mutations, whose filename sorts AFTER this one so the fixture is still intact here. Teardown
  is the runner's AfterClass sweep, so this feature is deliberately NOT tagged @cleanup — a per-scenario sweep
  would delete the shared fixture out from under the scenarios that follow.

  Runs once per tenant. What is under test is plane and pagination semantics, which are
  tenant-independent; devportal/comments.feature already runs the comment CRUD arc x2 tenants, so tenant routing
  for this capability is covered there.

  # ---------------------------------------------------------------------------------------------------------
  # PUBLISHER plane
  # ---------------------------------------------------------------------------------------------------------

  # Ports testPublisherGetAllCommentsTest.
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane root comment list returns both roots with the publisher entry point and no parent
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "publisher" comments of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.list[0].id" should be "{{ptRoot2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is root comment 2"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "PUBLISHER"
    And The response field "$.list[0].parentCommentId" should be null
    And The value of response field "$.list[1].id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is root comment 1"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "PUBLISHER"
    And The response field "$.list[1].parentCommentId" should be null

  # Ports testPublisherPaginatedRootCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane root comment list honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "publisher" comments of API "ptApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.offset" should be "1"
    And The value of response field "$.list[0].id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is root comment 1"

  # Ports testPublisherTotalCommentsOfPaginatedRootCommentsTest (which legacy guarded behind an if on total).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane root comment list reports the collection total independently of the page size
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "publisher" comments of API "ptApiId<suffix>" with limit 1 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.limit" should be "1"
    And The value of response field "$.pagination.offset" should be "0"
    And The value of response field "$.pagination.total" should be "2"

  # Ports the comment-with-replies half of testAddRepliesToRootCommentByAdminTest (PublisherCommentTest).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A publisher-plane comment fetched with its replies carries all three, each parented to it
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "content" should be "This is root comment 1"
    And The value of response field "category" should be "general"
    And The value of response field "entryPoint" should be "PUBLISHER"
    And The response field "parentCommentId" should be null
    And The value of response field "$.replies.count" should be "3"
    And The value of response field "$.replies.list[0].id" should be "{{ptReply1<suffix>}}"
    And The value of response field "$.replies.list[0].content" should be "This is a reply 1"
    And The value of response field "$.replies.list[0].category" should be "general"
    And The value of response field "$.replies.list[0].entryPoint" should be "PUBLISHER"
    And The value of response field "$.replies.list[0].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.replies.list[1].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.replies.list[1].content" should be "This is a reply 2"
    And The value of response field "$.replies.list[1].category" should be "general"
    And The value of response field "$.replies.list[1].entryPoint" should be "PUBLISHER"
    And The value of response field "$.replies.list[1].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.replies.list[2].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.replies.list[2].content" should be "This is a reply 3"
    And The value of response field "$.replies.list[2].category" should be "general"
    And The value of response field "$.replies.list[2].entryPoint" should be "PUBLISHER"
    And The value of response field "$.replies.list[2].parentCommentId" should be "{{ptRoot1<suffix>}}"

  # Ports testPublisherPaginatedCommentListTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The replies embedded in a publisher-plane comment honour a non-zero reply offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "$.replies.count" should be "2"
    And The value of response field "$.replies.pagination.offset" should be "1"
    And The value of response field "$.replies.pagination.total" should be "3"
    And The value of response field "$.replies.list[0].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.replies.list[0].content" should be "This is a reply 2"
    And The value of response field "$.replies.list[1].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.replies.list[1].content" should be "This is a reply 3"

  # Ports testPublisherGetRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane replies collection of a comment lists every reply with its parent and entry point
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" replies of comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "3"
    And The value of response field "$.list[0].id" should be "{{ptReply1<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 1"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[0].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[1].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 2"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[1].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[2].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.list[2].content" should be "This is a reply 3"
    And The value of response field "$.list[2].category" should be "general"
    And The value of response field "$.list[2].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[2].parentCommentId" should be "{{ptRoot1<suffix>}}"

  # Ports testPublisherPaginationOfRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane replies collection honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" replies of comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.pagination.offset" should be "1"
    And The value of response field "$.list[0].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 2"
    And The value of response field "$.list[1].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 3"

  # Ports testPublisherTotalRepliesOfPaginationOfRepliesOfCommentTest (legacy guarded it behind an if on total).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:publisher-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The publisher-plane replies collection reports the collection total independently of the page size
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" replies of comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with limit 1 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.limit" should be "1"
    And The value of response field "$.pagination.total" should be "3"

  # ---------------------------------------------------------------------------------------------------------
  # DEVPORTAL plane — only the reads devportal/comments.feature does NOT already cover. That feature asserts the
  # root count, the reply count and reply-offset visibility by substring; what is missing and added here is the
  # per-member exactness (id, category, entryPoint, parentCommentId), a non-zero offset on the ROOT list, and the
  # pagination totals.
  # ---------------------------------------------------------------------------------------------------------

  # Ports testDevPortalGetAllCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane root comment list returns both roots with the devportal entry point and no parent
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "devportal" comments of API "dtApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.list[0].id" should be "{{dtRoot2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is root comment 2"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "DEVPORTAL"
    And The response field "$.list[0].parentCommentId" should be null
    And The value of response field "$.list[1].id" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is root comment 1"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "DEVPORTAL"
    And The response field "$.list[1].parentCommentId" should be null

  # Ports testDevPortalPaginatedRootCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane root comment list honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "devportal" comments of API "dtApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.offset" should be "1"
    And The value of response field "$.list[0].id" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is root comment 1"

  # Ports testDevPortalTotalCommentsOfPaginatedRootCommentsTest (legacy guarded it behind an if on total).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane root comment list reports the collection total independently of the page size
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "devportal" comments of API "dtApiId<suffix>" with limit 1 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.limit" should be "1"
    And The value of response field "$.pagination.total" should be "2"

  # Ports the comment-with-replies half of testAddRepliesToRootCommentByAdminTest (DevPortalCommentTest).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A devportal-plane comment fetched with its replies carries all three, each parented to it
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "id" should be "{{dtRoot1<suffix>}}"
    And The value of response field "content" should be "This is root comment 1"
    And The value of response field "category" should be "general"
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The response field "parentCommentId" should be null
    And The value of response field "$.replies.count" should be "3"
    And The value of response field "$.replies.list[0].id" should be "{{dtReply1<suffix>}}"
    And The value of response field "$.replies.list[0].content" should be "This is a reply 1"
    And The value of response field "$.replies.list[0].category" should be "general"
    And The value of response field "$.replies.list[0].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.replies.list[0].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.replies.list[1].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.replies.list[1].content" should be "This is a reply 2"
    And The value of response field "$.replies.list[1].category" should be "general"
    And The value of response field "$.replies.list[1].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.replies.list[1].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.replies.list[2].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.replies.list[2].content" should be "This is a reply 3"
    And The value of response field "$.replies.list[2].category" should be "general"
    And The value of response field "$.replies.list[2].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.replies.list[2].parentCommentId" should be "{{dtRoot1<suffix>}}"

  # Ports testDevPortalPaginatedCommentListTest — the exact-count/exact-index form of the substring reply-offset
  # check devportal/comments.feature already makes.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The replies embedded in a devportal-plane comment honour a non-zero reply offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with reply limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "$.replies.count" should be "2"
    And The value of response field "$.replies.pagination.offset" should be "1"
    And The value of response field "$.replies.pagination.total" should be "3"
    And The value of response field "$.replies.list[0].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.replies.list[0].content" should be "This is a reply 2"
    And The value of response field "$.replies.list[1].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.replies.list[1].content" should be "This is a reply 3"

  # Ports testDevPortalGetRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane replies collection of a comment lists every reply with its parent and entry point
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" replies of comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "3"
    And The value of response field "$.list[0].id" should be "{{dtReply1<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 1"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[0].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[1].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 2"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[1].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[2].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.list[2].content" should be "This is a reply 3"
    And The value of response field "$.list[2].category" should be "general"
    And The value of response field "$.list[2].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[2].parentCommentId" should be "{{dtRoot1<suffix>}}"

  # Ports testDevPortalPaginationOfRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane replies collection honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" replies of comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.pagination.offset" should be "1"
    And The value of response field "$.list[0].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 2"
    And The value of response field "$.list[1].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 3"

  # Ports testDevPortalTotalRepliesOfPaginationOfRepliesOfCommentTest (legacy guarded it behind an if on total).

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:devportal-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The devportal-plane replies collection reports the collection total independently of the page size
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" replies of comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with limit 1 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.pagination.limit" should be "1"
    And The value of response field "$.pagination.total" should be "3"

  # ---------------------------------------------------------------------------------------------------------
  # CROSS-PLANE reads — the same thread read from the OTHER plane. The entryPoint travels with the comment, so a
  # devportal read of a publisher-entered thread still reports PUBLISHER (and vice versa): the plane a read is
  # issued on does not rewrite where a comment came from.
  # ---------------------------------------------------------------------------------------------------------

  # Ports testVerifyDevPortalGetAllCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A devportal-plane list of a publisher-entered thread reports the publisher entry point
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "devportal" comments of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.pagination.total" should be "2"
    And The value of response field "$.list[0].id" should be "{{ptRoot2<suffix>}}"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "PUBLISHER"
    And The response field "$.list[0].parentCommentId" should be null
    And The value of response field "$.list[1].id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "PUBLISHER"
    And The response field "$.list[1].parentCommentId" should be null

  # Ports testVerifyDevPortalPaginatedRootCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A devportal-plane list of a publisher-entered thread honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "devportal" comments of API "ptApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.list[0].id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is root comment 1"

  # Ports testVerifyDevPortalGetRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The devportal-plane replies collection of a publisher-entered comment lists every reply
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" replies of comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "3"
    And The value of response field "$.pagination.total" should be "3"
    And The value of response field "$.list[0].id" should be "{{ptReply1<suffix>}}"
    And The value of response field "$.list[0].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[0].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[1].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.list[1].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[1].parentCommentId" should be "{{ptRoot1<suffix>}}"
    And The value of response field "$.list[2].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.list[2].entryPoint" should be "PUBLISHER"
    And The value of response field "$.list[2].parentCommentId" should be "{{ptRoot1<suffix>}}"

  # Ports testVerifyDevPortalPaginationOfRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: The devportal-plane replies collection of a publisher-entered comment honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "devportal" replies of comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.list[0].id" should be "{{ptReply2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 2"
    And The value of response field "$.list[1].id" should be "{{ptReply3<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 3"

  # Ports testVerifyPublisherGetAllCommentsTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A publisher-plane list of a devportal-entered thread reports the devportal entry point
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve all "publisher" comments of API "dtApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.pagination.total" should be "2"
    And The value of response field "$.list[0].id" should be "{{dtRoot2<suffix>}}"
    And The value of response field "$.list[0].category" should be "general"
    And The value of response field "$.list[0].entryPoint" should be "DEVPORTAL"
    And The response field "$.list[0].parentCommentId" should be null
    And The value of response field "$.list[1].id" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[1].category" should be "general"
    And The value of response field "$.list[1].entryPoint" should be "DEVPORTAL"
    And The response field "$.list[1].parentCommentId" should be null

  # Ports testVerifyPublisherPaginatedCommentListTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A publisher-plane read of a devportal-entered comment honours a non-zero reply offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with reply limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The value of response field "$.replies.count" should be "2"
    And The value of response field "$.replies.list[0].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.replies.list[0].content" should be "This is a reply 2"
    And The value of response field "$.replies.list[1].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.replies.list[1].content" should be "This is a reply 3"

  # Ports testVerifyPublisherGetRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The publisher-plane replies collection of a devportal-entered comment lists every reply
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" replies of comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "3"
    And The value of response field "$.pagination.total" should be "3"
    And The value of response field "$.list[0].id" should be "{{dtReply1<suffix>}}"
    And The value of response field "$.list[0].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[0].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[1].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.list[1].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[1].parentCommentId" should be "{{dtRoot1<suffix>}}"
    And The value of response field "$.list[2].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.list[2].entryPoint" should be "DEVPORTAL"
    And The value of response field "$.list[2].parentCommentId" should be "{{dtRoot1<suffix>}}"

  # Ports testVerifyPublisherPaginationOfRepliesOfCommentTest.

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: The publisher-plane replies collection of a devportal-entered comment honours a non-zero offset
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I retrieve the "publisher" replies of comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with limit 3 offset 1
    Then The response status code should be 200
    And The value of response field "count" should be "2"
    And The value of response field "$.list[0].id" should be "{{dtReply2<suffix>}}"
    And The value of response field "$.list[0].content" should be "This is a reply 2"
    And The value of response field "$.list[1].id" should be "{{dtReply3<suffix>}}"
    And The value of response field "$.list[1].content" should be "This is a reply 3"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
