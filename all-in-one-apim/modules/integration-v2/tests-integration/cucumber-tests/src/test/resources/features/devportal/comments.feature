@cleanup
Feature: DevPortal API Comments

  Comment management on a published API: adding root comments and threaded replies, listing them with
  limit/offset pagination (roots, replies, and a comment-with-its-replies), editing a comment's content and
  category, cascade-deletion (deleting a root removes its replies), the not-found response for a deleted
  comment, cross-plane visibility (a comment added on one plane is visible on the other, tracked by its
  entryPoint), and the moderator permission model (owner and admins may edit/delete; a non-owner non-admin
  may not). Comments are a sub-resource of the API and cascade-delete with it — teardown via the @cleanup hook.
  Ports DevPortalCommentTest and PublisherCommentTest.

  # Core comment CRUD + threaded replies + pagination, driven from the devportal plane. Mirrors the dependent
  # chain of DevPortalCommentTest: add 2 roots, add 3 replies to the first, list roots/replies with pagination,
  # edit, then cascade-delete. Runs as the tenant admin (owns both the publisher API-create and the devportal
  # comment scopes), in both tenants.
  @cap:devportal @feat:comments @type:regression @rule:crud @dep:publisher @legacy:DevPortalCommentTest
  Scenario Outline: Add, list, paginate, edit and cascade-delete devportal comments as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "cmApiId" and deployed it
    When I publish the "apis" resource with id "cmApiId"
    Then The lifecycle status of API "cmApiId" should be "Published"

    # Two root comments (add returns 201; the entryPoint is DEVPORTAL for a devportal-plane comment).
    When I add a "devportal" comment "This is root comment 1" with category "general" to API "cmApiId" as "root1"
    Then The response status code should be 201
    And The response should contain "\"entryPoint\":\"DEVPORTAL\""
    When I add a "devportal" comment "This is root comment 2" with category "general" to API "cmApiId" as "root2"
    Then The response status code should be 201

    # Three replies to the first root.
    When I add a "devportal" reply "This is a reply 1" to comment "root1" of API "cmApiId" as "reply1"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply 2" to comment "root1" of API "cmApiId" as "reply2"
    Then The response status code should be 201
    When I add a "devportal" reply "This is a reply 3" to comment "root1" of API "cmApiId" as "reply3"
    Then The response status code should be 201

    # The root comment fetched with its replies carries all three replies.
    When I retrieve the "devportal" comment "root1" of API "cmApiId" with reply limit 3 offset 0
    Then The response status code should be 200
    And The response should contain "This is root comment 1"
    And The response should contain "This is a reply 1"
    And The response should contain "This is a reply 3"

    # Reply pagination: offset 1 skips the first reply.
    When I retrieve the "devportal" comment "root1" of API "cmApiId" with reply limit 3 offset 1
    Then The response status code should be 200
    And The response should contain "This is a reply 2"
    And The response should not contain "This is a reply 1"

    # All root comments: exactly the two roots (count 2).
    When I retrieve all "devportal" comments of API "cmApiId" with limit 5 offset 0
    Then The response status code should be 200
    And The response should contain "\"count\":2"
    And The response should contain "This is root comment 1"
    And The response should contain "This is root comment 2"

    # Replies list of the root: count 3.
    When I retrieve the "devportal" replies of comment "root1" of API "cmApiId" with limit 5 offset 0
    Then The response status code should be 200
    And The response should contain "\"count\":3"

    # A publisher-plane read sees the same devportal comments (cross-plane visibility).
    When I retrieve all "publisher" comments of API "cmApiId" with limit 5 offset 0
    Then The response status code should be 200
    And The response should contain "This is root comment 1"

    # Edit the root comment's content and category.
    When I edit the "devportal" comment "root1" of API "cmApiId" to content "Edited root comment" category "bug fix"
    Then The response status code should be 200
    And The response should contain "Edited root comment"
    And The response should contain "bug fix"
    And The response should contain "updatedTime"

    # Delete the root comment — its replies cascade away (each reply is then 404).
    When I delete the "devportal" comment "root1" of API "cmApiId"
    Then The response status code should be 200
    When I retrieve the "devportal" comment "reply1" of API "cmApiId" with reply limit 3 offset 0
    Then The response status code should be 404

    # Deleting an already-deleted comment is a 404.
    When I delete the "devportal" comment "root1" of API "cmApiId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Publisher-plane entry-point: a comment created via the PUBLISHER plane carries entryPoint "publisher" and is
  # visible from the devportal. Ports the distinguishing behaviour of PublisherCommentTest (the devportal chain
  # above already covers the shared CRUD/pagination surface). Runs in both tenants.
  @cap:devportal @feat:comments @type:regression @rule:entry-point @dep:publisher @legacy:PublisherCommentTest
  Scenario Outline: A comment added on the publisher plane is visible on the devportal as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "pcApiId" and deployed it
    When I publish the "apis" resource with id "pcApiId"
    Then The lifecycle status of API "pcApiId" should be "Published"

    When I add a "publisher" comment "This is a publisher comment" with category "general" to API "pcApiId" as "pubRoot"
    Then The response status code should be 201
    And The response should contain "\"entryPoint\":\"PUBLISHER\""

    # A reply from the publisher plane, then verify both are visible from the devportal.
    When I add a "publisher" reply "This is a publisher reply" to comment "pubRoot" of API "pcApiId" as "pubReply"
    Then The response status code should be 201
    When I retrieve the "devportal" comment "pubRoot" of API "pcApiId" with reply limit 5 offset 0
    Then The response status code should be 200
    And The response should contain "This is a publisher comment"
    And The response should contain "This is a publisher reply"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Moderator permission model: a comment's OWNER (and any admin) may edit/delete it, but a NON-owner NON-admin
  # may not (403). Ports the permission-matrix methods of DevPortalCommentTest, which the legacy gated to the
  # super tenant only. The owner (admin) owns the comment; the subscriber (Internal/subscriber, non-admin) is the
  # rejected non-owner. Runs x2-tenant (super + tenant1): the enforcement is identity-based, exercised per tenant.
  @cap:devportal @feat:comments @type:negative @rule:moderation @dep:publisher @legacy:DevPortalCommentTest @legacy:PublisherCommentTest
  Scenario Outline: A non-owner non-admin user cannot edit or delete another user's comment as <owner>
    Given The system is ready
    And I have valid access tokens as "<owner>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "modApiId" and deployed it
    When I publish the "apis" resource with id "modApiId"
    Then The lifecycle status of API "modApiId" should be "Published"

    # The admin adds a comment (the admin is the owner).
    When I add a "devportal" comment "Admin owned comment" with category "general" to API "modApiId" as "modRoot"
    Then The response status code should be 201

    # A non-owner non-admin (subscriber) is refused edit (403) and delete (403). Switch the acting actor to the
    # subscriber, register its DCR client, and mint its devportal token so the comment steps authenticate as the
    # subscriber.
    Given I act as "<nonOwner>"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I edit the "devportal" comment "modRoot" of API "modApiId" to content "Hijacked" category "general"
    Then The response status code should be 403
    When I delete the "devportal" comment "modRoot" of API "modApiId"
    Then The response status code should be 403

    # The owning actor can still edit and delete it.
    Given I act as "<owner>"
    When I edit the "devportal" comment "modRoot" of API "modApiId" to content "Owner edit" category "general"
    Then The response status code should be 200
    When I delete the "devportal" comment "modRoot" of API "modApiId"
    Then The response status code should be 200

    Examples:
      | owner             | nonOwner                   |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
