Feature: API Comment Threads — Edit, Cascade Delete and Moderation Across Both Planes

  Everything that CHANGES a comment thread: the edit contract (what a PATCH returns, and exactly when
  updatedTime moves), cross-plane replies, cascade deletion driven from either plane, and the authorisation model
  around another user's comment — where moderation is delete-only by design (a non-owner holding the moderator
  scope may DELETE another user's comment but not EDIT it, which is what the shipped OAS declares on both planes).
  Ports the mutating half of PublisherCommentTest and DevPortalCommentTest.

  Ordering — this file is deliberately ordered and the dependencies are real:
    * Its filename sorts AFTER comments_1_thread_reads, so every read assertion has already run against the
      pristine fixture before anything here mutates it (cucumber-testng orders features lexicographically by
      filename, not by the runner's array order).
    * Within the file: the two edit scenarios come first (they mutate a root's content, which the reads pinned);
      the cross-plane REPLY scenarios come next, because each targets a root that a later scenario DELETES; the
      cascade-delete scenarios come last of the fixture-consuming set. The moderation scenarios at the end create
      their own comments on the comment-free mtApiId<suffix> and are therefore order-independent.

  updatedTime and why no wait is needed: updatedTime is stored with MILLISECOND precision (observed
  "2026-08-05 01:58:46.133" / ".144" / ".154" for three consecutive edits), so two edits separated by a REST
  round trip land on distinct values without any delay. The legacy test slept two seconds between edits "due to
  update time assertions"; that sleep was unnecessary here and is an anti-pattern in this suite, so it is simply
  absent — the assertion that consecutive edits produce different timestamps stands on its own.

  Teardown is the runner's AfterClass sweep, so this feature is deliberately NOT tagged @cleanup — a per-scenario
  sweep would delete the shared fixture out from under the scenarios that follow. Runs x2 tenants, extending the
  legacy tests, which gated every authorisation case to the super-tenant admin mode.

  # ---------------------------------------------------------------------------------------------------------
  # Edit semantics
  # ---------------------------------------------------------------------------------------------------------

  # Ports testPublisherEditCommentTest — all three dataProvider rows (content only, category only, both) plus the
  # no-op re-edit. The extract step is itself the "updatedTime is not null" assertion: it fails if the field is
  # absent or null.
  @cap:devportal @feat:comments @type:regression @rule:edit @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: Editing a publisher-plane comment returns the new values and moves updatedTime only on a real change as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    # Row 1 — content only.
    When I edit the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" to content "Edited root comment" category "general"
    Then The response status code should be 200
    And The value of response field "id" should be "{{ptRoot1<suffix>}}"
    And The value of response field "content" should be "Edited root comment"
    And The value of response field "category" should be "general"
    And I extract response field "updatedTime" and store it as "ptEditTime1<suffix>"

    # Row 2 — category only. The value changed, so updatedTime must differ from the previous edit's.
    When I edit the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" to content "Edited root comment" category "bug fix"
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment"
    And The value of response field "category" should be "bug fix"
    And I extract response field "updatedTime" and store it as "ptEditTime2<suffix>"
    And The stored value "ptEditTime2<suffix>" should not equal "ptEditTime1<suffix>"

    # Row 3 — content AND category.
    When I edit the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" to content "Edited root comment 1" category "general bug fix"
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment 1"
    And The value of response field "category" should be "general bug fix"
    And I extract response field "updatedTime" and store it as "ptEditTime3<suffix>"
    And The stored value "ptEditTime3<suffix>" should not equal "ptEditTime2<suffix>"

    # Re-editing to the SAME content and category changes nothing: 200 with NO body, and updatedTime stands still.
    When I edit the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" to content "Edited root comment 1" category "general bug fix"
    Then The response status code should be 200
    And The response body should be empty
    When I retrieve the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment 1"
    And The value of response field "category" should be "general bug fix"
    And I extract response field "updatedTime" and store it as "ptNoopTime<suffix>"
    And The stored value "ptNoopTime<suffix>" should equal "ptEditTime3<suffix>"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalEditCommentTest — the same contract entered through the devportal plane.
  @cap:devportal @feat:comments @type:regression @rule:edit @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: Editing a devportal-plane comment returns the new values and moves updatedTime only on a real change as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I edit the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" to content "Edited root comment" category "general"
    Then The response status code should be 200
    And The value of response field "id" should be "{{dtRoot1<suffix>}}"
    And The value of response field "content" should be "Edited root comment"
    And The value of response field "category" should be "general"
    And I extract response field "updatedTime" and store it as "dtEditTime1<suffix>"

    When I edit the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" to content "Edited root comment" category "bug fix"
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment"
    And The value of response field "category" should be "bug fix"
    And I extract response field "updatedTime" and store it as "dtEditTime2<suffix>"
    And The stored value "dtEditTime2<suffix>" should not equal "dtEditTime1<suffix>"

    When I edit the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" to content "Edited root comment 1" category "general bug fix"
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment 1"
    And The value of response field "category" should be "general bug fix"
    And I extract response field "updatedTime" and store it as "dtEditTime3<suffix>"
    And The stored value "dtEditTime3<suffix>" should not equal "dtEditTime2<suffix>"

    When I edit the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" to content "Edited root comment 1" category "general bug fix"
    Then The response status code should be 200
    And The response body should be empty
    When I retrieve the "devportal" comment "dtRoot1<suffix>" of API "dtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Edited root comment 1"
    And The value of response field "category" should be "general bug fix"
    And I extract response field "updatedTime" and store it as "dtNoopTime<suffix>"
    And The stored value "dtNoopTime<suffix>" should equal "dtEditTime3<suffix>"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # ---------------------------------------------------------------------------------------------------------
  # Cross-plane replies. A reply records the plane IT was entered from, not its parent's — so the two are free to
  # differ within one thread. Each of these targets a root that a later cascade-delete scenario removes, which is
  # why they precede the deletes.
  # ---------------------------------------------------------------------------------------------------------
  # Ports testPublisherAdminUserAddReplyToCommentFromDevPortalTest.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A publisher-plane reply can be added to a devportal-entered root comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "publisher" reply "This is a reply from admin user from Publisher" to comment "dtRoot2<suffix>" of API "dtApiId<suffix>" as "dtXplaneReply<suffix>"
    Then The response status code should be 201
    And The value of response field "content" should be "This is a reply from admin user from Publisher"
    And The value of response field "entryPoint" should be "PUBLISHER"
    And The value of response field "parentCommentId" should be "{{dtRoot2<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalAdminUserAddReplyToCommentFromPublisherTest.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A devportal-plane reply can be added to a publisher-entered root comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "devportal" reply "This is a reply from admin user from DevPortal" to comment "ptRoot2<suffix>" of API "ptApiId<suffix>" as "ptXplaneReply<suffix>"
    Then The response status code should be 201
    And The value of response field "content" should be "This is a reply from admin user from DevPortal"
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The value of response field "parentCommentId" should be "{{ptRoot2<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # ---------------------------------------------------------------------------------------------------------
  # Cascade deletion
  # ---------------------------------------------------------------------------------------------------------
  # Ports testPublisherDeleteCommentTest and testPublisherDeleteNotExistingCommentTest. All THREE replies are
  # checked, not just one: a cascade that removes the first reply and orphans the rest would otherwise pass.
  @cap:devportal @feat:comments @type:regression @rule:cascade-delete @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: Deleting a publisher-plane root comment removes every one of its replies and is then not found as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I delete the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>"
    Then The response status code should be 200
    When I retrieve the "publisher" comment "ptReply1<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve the "publisher" comment "ptReply2<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve the "publisher" comment "ptReply3<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    # The root itself is gone: only one root remains, and deleting it again is a 404.
    When I retrieve all "publisher" comments of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.list[0].id" should be "{{ptRoot2<suffix>}}"
    When I delete the "publisher" comment "ptRoot1<suffix>" of API "ptApiId<suffix>"
    Then The response status code should be 404

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testVerifyDevPortalAdminDeleteCommentTest — the delete is issued on the plane the comment did NOT come
  # from, and still cascades.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A publisher-entered root comment can be deleted from the devportal plane and cascades there too as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I delete the "devportal" comment "ptRoot2<suffix>" of API "ptApiId<suffix>"
    Then The response status code should be 200
    When I retrieve the "devportal" comment "ptRoot2Reply<suffix>" of API "ptApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve all "devportal" comments of API "ptApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "0"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testVerifyPublisherAdminDeleteCommentTest — the mirror direction.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A devportal-entered root comment can be deleted from the publisher plane and cascades there too as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I delete the "publisher" comment "dtRoot2<suffix>" of API "dtApiId<suffix>"
    Then The response status code should be 200
    When I retrieve the "publisher" comment "dtRoot2Reply<suffix>" of API "dtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve all "publisher" comments of API "dtApiId<suffix>" with limit 5 offset 0
    Then The response status code should be 200
    And The value of response field "count" should be "1"
    And The value of response field "$.list[0].id" should be "{{dtRoot1<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # ---------------------------------------------------------------------------------------------------------
  # Authorisation model. Every scenario below builds its own comments on mtApiId<suffix> (which the fixture leaves
  # comment-free), so none of them depends on another's state or on the counted threads above.
  #
  # The actors: publisherUser is Internal/creator,publisher — a NON-admin that the tenant-conf scope mapping does
  # grant apim:comment_view/apim:comment_manage, so it can work the publisher plane; subscriberUser is
  # Internal/subscriber, the devportal consumer. admin is the tenant admin.
  #
  # Moderating ANOTHER user's comment turns on the apim:admin scope, which both API definitions declare on the
  # comment PATCH/DELETE as the "special scope added to moderate other comments" — hence the explicit
  # "as a comment moderator" steps, which mint a comment token carrying it. It is a property of the TOKEN, not of
  # the role: a tenant admin holding only the ordinary comment scopes is refused exactly like any other non-owner.
  # It also only reaches DELETE — the edit handlers gate on ownership alone. See the two moderator scenarios.
  # ---------------------------------------------------------------------------------------------------------
  # Ports testPublisherAddCommentByNonAdminUserTest + testPublisherAddReplyToNonAdminUserCommentByAdminUserTest.
  # The first half is what proves an ordinary user can comment at all — the rest of the corpus only ever uses a
  # non-admin as the REJECTED actor.
  #
  # An OUTLINE over the comment's author, whose SECOND row is a SECONDARY.COM user-store creator (CLAUDE.md §12) —
  # closing the legacy SUPER_TENANT_USER_STORE_USER mode for the comment-authorship facet. Authorship is the right
  # place for it: every moderation decision in this file turns on the stored comment owner being compared to the
  # caller, so createdBy is the field that must carry a store-qualified username verbatim. The row is a real probe
  # rather than a repeat because it also proves a store user can obtain a DCR application and a comment-scoped
  # token at all. The outline's LAST row ends acting as "admin@tenant1.com"; the scenarios below open with
  # their own auth composite rather than inheriting that actor (CLAUDE.md §12).
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A non-admin creator can add a publisher-plane comment and the admin can reply to it as <creator>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    Given I act as "<creator>"
    And I have a valid DCR application for the current user
    And I store the username of actor "<creator>" as "nonAdminPubAuthor<suffix>"
    When I add a "publisher" comment "This is root comment by non admin user" with category "general" to API "mtApiId<suffix>" as "nonAdminPubRoot<suffix>"
    Then The response status code should be 201
    And The value of response field "content" should be "This is root comment by non admin user"
    And The value of response field "createdBy" should be "{{nonAdminPubAuthor<suffix>}}"
    And The value of response field "entryPoint" should be "PUBLISHER"
    And The response field "parentCommentId" should be null
    Given I act as "<actor>"
    When I add a "publisher" reply "This is a reply from admin user" to comment "nonAdminPubRoot<suffix>" of API "mtApiId<suffix>" as "adminReplyToNonAdmin"
    Then The response status code should be 201
    And The value of response field "parentCommentId" should be "{{nonAdminPubRoot<suffix>}}"

    Examples:
      | actor             | suffix       | creator                                      |
      | admin             |              | publisherUser                                |
      | admin             |              | SECONDARY.COM/publisherUser1                 |
      | admin@tenant1.com | @tenant1.com | publisherUser@tenant1.com                    |
      | admin@tenant1.com | @tenant1.com | SECONDARY.COM/publisherUser1@tenant1.com     |

  # Ports testPublisherAddReplyToAdminUserCommentByNonAdminUserTest.
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A non-admin creator can reply to the admin's publisher-plane comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "publisher" comment "Admin root for a non admin reply" with category "general" to API "mtApiId<suffix>" as "adminPubRootForNonAdmin"
    Then The response status code should be 201
    Given I act as "publisherUser<suffix>"
    And I have a valid DCR application for the current user
    When I add a "publisher" reply "This is a reply from non admin user" to comment "adminPubRootForNonAdmin" of API "mtApiId<suffix>" as "nonAdminReplyToAdmin<suffix>"
    Then The response status code should be 201
    And The value of response field "content" should be "This is a reply from non admin user"
    And The value of response field "entryPoint" should be "PUBLISHER"
    And The value of response field "parentCommentId" should be "{{adminPubRootForNonAdmin}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testPublisherEditCommentByNonOwnerNonAdminUserTest + testPublisherDeleteCommentByNonOwnerNonAdminUserTest.
  @cap:devportal @feat:comments @type:negative @rule:moderation @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A non-owner non-admin user can neither edit nor delete a publisher-plane comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "publisher" comment "Admin owned publisher comment" with category "general" to API "mtApiId<suffix>" as "adminOwnedPubRoot<suffix>"
    Then The response status code should be 201
    Given I act as "publisherUser<suffix>"
    And I have a valid DCR application for the current user
    When I edit the "publisher" comment "adminOwnedPubRoot<suffix>" of API "mtApiId<suffix>" to content "Edited root comment by non owner non admin user" category "general"
    Then The response status code should be 403
    When I delete the "publisher" comment "adminOwnedPubRoot<suffix>" of API "mtApiId<suffix>"
    Then The response status code should be 403
    # Still intact and unchanged after both refusals.
    Given I act as "<actor>"
    When I retrieve the "publisher" comment "adminOwnedPubRoot<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Admin owned publisher comment"
    And The response field "updatedTime" should be null

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testPublisherEditCommentByNonOwnerAdminUserTest + testPublisherDeleteCommentByNonOwnerAdminUserTest.
  # Legacy could not actually test either: its "non-owner admin" client was constructed from the SAME user as the
  # comment's owner, so its 200s were the OWNER path and the non-owner path was never exercised. With a comment
  # that genuinely belongs to publisherUser, the real rule is visible: moderation is DELETE-ONLY BY DESIGN. The two
  # halves are asserted back-to-back on ONE comment because the contrast IS the rule (asserting them apart would
  # read as two unrelated facts):
  #   * DELETE honours the apim:admin scope — a non-owner holding it may remove another user's comment.
  #   * EDIT does not. The publisher and devportal comment-edit handlers both gate on ownership ALONE
  #     (comment.getUser().equals(username)) with no admin-scope branch, so a non-owner is refused 403 EVEN WITH
  #     apim:admin in the token. That the token really carries the scope is asserted when it is minted, so this
  #     403 cannot be a silently-dropped scope.
  #
  # This is the SPECIFIED contract, not an accident — the shipped OAS puts the intent on the very line that creates
  # the asymmetry. On /apis/{apiId}/comments/{commentId}, DELETE declares apim:admin with the inline note
  # "special scope added to moderate other comments as well" (publisher-api.yaml; devportal-api.yaml says
  # "special scope added to moderate comments"), while PATCH declares no apim:admin on EITHER plane. So a
  # moderator is meant to be able to remove another user's comment and not to rewrite it — asserting a 200 on the
  # edit would be asserting a capability the contract never grants.
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A comment moderator may delete but not edit another user's publisher-plane comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    Given I act as "publisherUser<suffix>"
    And I have a valid DCR application for the current user
    When I add a "publisher" comment "Non admin owned root" with category "general" to API "mtApiId<suffix>" as "moderatedPubRoot<suffix>"
    Then The response status code should be 201
    When I add a "publisher" reply "Non admin owned reply" to comment "moderatedPubRoot<suffix>" of API "mtApiId<suffix>" as "moderatedPubReply<suffix>"
    Then The response status code should be 201

    # Editing someone else's comment is refused even with the moderator scope, and leaves it untouched.
    Given I act as "<actor>"
    When I edit the "publisher" comment "moderatedPubRoot<suffix>" of API "mtApiId<suffix>" to content "Edited root comment by non owner admin user" category "new_general" as a comment moderator
    Then The response status code should be 403
    When I retrieve the "publisher" comment "moderatedPubRoot<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Non admin owned root"
    And The value of response field "category" should be "general"
    And The response field "updatedTime" should be null

    # Deleting it IS permitted with the moderator scope, and cascades to its replies.
    When I delete the "publisher" comment "moderatedPubRoot<suffix>" of API "mtApiId<suffix>" as a comment moderator
    Then The response status code should be 200
    When I retrieve the "publisher" comment "moderatedPubRoot<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve the "publisher" comment "moderatedPubReply<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalAddCommentByNonAdminUserTest + testDevPortalAddReplyToNonAdminUserCommentByAdminUserTest.
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A subscriber can add a devportal-plane comment and the admin can reply to it as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    Given I act as "subscriberUser<suffix>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I add a "devportal" comment "This is root comment by non admin user" with category "general" to API "mtApiId<suffix>" as "nonAdminDevRoot<suffix>"
    Then The response status code should be 201
    And The value of response field "content" should be "This is root comment by non admin user"
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The response field "parentCommentId" should be null
    Given I act as "<actor>"
    When I add a "devportal" reply "This is a reply from admin user" to comment "nonAdminDevRoot<suffix>" of API "mtApiId<suffix>" as "adminDevReplyToNonAdmin"
    Then The response status code should be 201
    And The value of response field "parentCommentId" should be "{{nonAdminDevRoot<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalAddReplyToAdminUserCommentByNonAdminUserTest.
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A subscriber can reply to the admin's devportal-plane comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "devportal" comment "Admin root for a subscriber reply" with category "general" to API "mtApiId<suffix>" as "adminDevRootForNonAdmin"
    Then The response status code should be 201
    Given I act as "subscriberUser<suffix>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I add a "devportal" reply "This is a reply from non admin user" to comment "adminDevRootForNonAdmin" of API "mtApiId<suffix>" as "nonAdminDevReplyToAdmin<suffix>"
    Then The response status code should be 201
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The value of response field "parentCommentId" should be "{{adminDevRootForNonAdmin}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalEditCommentByNonOwnerAdminUserTest + testDevPortalDeleteCommentByNonOwnerAdminUserTest.
  # Same asymmetry as on the publisher plane, and for the same reason — see that scenario's note.
  @cap:devportal @feat:comments @type:regression @rule:moderation @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A comment moderator may delete but not edit another user's devportal-plane comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    Given I act as "subscriberUser<suffix>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I add a "devportal" comment "Subscriber owned root" with category "general" to API "mtApiId<suffix>" as "moderatedDevRoot<suffix>"
    Then The response status code should be 201
    When I add a "devportal" reply "Subscriber owned reply" to comment "moderatedDevRoot<suffix>" of API "mtApiId<suffix>" as "moderatedDevReply<suffix>"
    Then The response status code should be 201

    Given I act as "<actor>"
    When I edit the "devportal" comment "moderatedDevRoot<suffix>" of API "mtApiId<suffix>" to content "Edited root comment by non owner admin user" category "new_general" as a comment moderator
    Then The response status code should be 403
    When I retrieve the "devportal" comment "moderatedDevRoot<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 200
    And The value of response field "content" should be "Subscriber owned root"
    And The value of response field "category" should be "general"
    And The response field "updatedTime" should be null

    When I delete the "devportal" comment "moderatedDevRoot<suffix>" of API "mtApiId<suffix>" as a comment moderator
    Then The response status code should be 200
    When I retrieve the "devportal" comment "moderatedDevRoot<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404
    When I retrieve the "devportal" comment "moderatedDevReply<suffix>" of API "mtApiId<suffix>" with reply limit 3 offset 0
    Then The response status code should be 404

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testDevPortalNonAdminUserAddReplyToCommentFromPublisherTest — a non-admin crossing planes inbound.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A subscriber can add a devportal-plane reply to a publisher-entered comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "publisher" comment "Publisher root for a subscriber reply" with category "general" to API "mtApiId<suffix>" as "pubRootForSubReply<suffix>"
    Then The response status code should be 201
    Given I act as "subscriberUser<suffix>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I add a "devportal" reply "This is a reply from non admin user from DevPortal" to comment "pubRootForSubReply<suffix>" of API "mtApiId<suffix>" as "subDevReplyOnPubRoot<suffix>"
    Then The response status code should be 201
    And The value of response field "entryPoint" should be "DEVPORTAL"
    And The value of response field "parentCommentId" should be "{{pubRootForSubReply<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |

  # Ports testPublisherNonAdminUserAddReplyToCommentFromDevPortalTest — the mirror direction.
  @cap:devportal @feat:comments @type:regression @rule:cross-plane @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: A non-admin creator can add a publisher-plane reply to a devportal-entered comment as <actor>
    Given The system is ready and I have valid publisher access tokens as "<actor>"
    When I add a "devportal" comment "Devportal root for a creator reply" with category "general" to API "mtApiId<suffix>" as "devRootForPubReply<suffix>"
    Then The response status code should be 201
    Given I act as "publisherUser<suffix>"
    And I have a valid DCR application for the current user
    When I add a "publisher" reply "This is a reply from non admin user from Publisher" to comment "devRootForPubReply<suffix>" of API "mtApiId<suffix>" as "pubReplyOnDevRoot<suffix>"
    Then The response status code should be 201
    And The value of response field "entryPoint" should be "PUBLISHER"
    And The value of response field "parentCommentId" should be "{{devRootForPubReply<suffix>}}"

    Examples:
      | actor                     | suffix       |
      | admin                     |              |
      | admin@tenant1.com         | @tenant1.com |
