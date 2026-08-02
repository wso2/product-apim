@cleanup
Feature: Approval workflow - DevPortal user self sign-up

  With the UserSignUp Approval executor active, a user who self-signs-up through the product's self-registration
  API parks in an AM_USER_SIGNUP pending task instead of becoming a subscriber immediately: it is created carrying
  only Internal/selfsignup, so its DevPortal token is issued WITHOUT the subscriber scopes and the restricted
  DevPortal application-list page rejects it. Approving the task grants the sign-up roles, after which a freshly
  minted token for the SAME credential reaches that page. Rejecting a sign-up instead removes the user from the
  user store altogether. Ports WorkflowApprovalExecutorTest#testUserSignUpWorkflowProcess.

  The sign-up is a product operation performed as an actor, and the new user is adopted as a runtime actor, so
  its tokens come from the standard composites and the cleanup sweep deletes it as its owning tenant's admin.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: A pending self-signup user cannot reach the DevPortal application list until approved
    Given The system is ready
    And I have valid access tokens as "admin"

    When I self-sign-up a DevPortal user with password "Signup#12345" as actor "signupApproveUser" storing the username as "signupApproveName"
    Then The response status code should be 201
    And the user "{{signupApproveName}}" in tenant "carbon.super" should exist

    # The sign-up parks pending; admin sees the task keyed by the tenant-aware username.
    When I capture the pending "AM_USER_SIGNUP" workflow reference where "tenantAwareUserName" is "{{signupApproveName}}" as "signupWfRef"
    And I get the workflow with reference "signupWfRef"
    Then The response status code should be 200
    And The workflow property "tenantAwareUserName" should be "{{signupApproveName}}"

    # While pending the user holds no subscriber role, so its DevPortal token is refused the restricted page.
    Given I act as "signupApproveUser"
    And I have a valid DCR application for the current user
    And I have a valid Devportal access token for the current user
    When I list the DevPortal applications
    Then The response status code should be 401

    # Admin approves; the sign-up roles are granted.
    Given I act as "admin"
    When I "APPROVED" the workflow with reference "signupWfRef"
    Then The response status code should be 200

    # A freshly minted token for the same credential now carries the subscriber scopes and reaches the page.
    Given I act as "signupApproveUser"
    And I have a valid Devportal access token for the current user
    When I list the DevPortal applications
    Then The response status code should be 200

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Rejecting a self-signup request removes the user from the user store
    Given The system is ready
    And I have valid access tokens as "admin"

    When I self-sign-up a DevPortal user with password "Signup#12345" as actor "signupRejectUser" storing the username as "signupRejectName"
    Then The response status code should be 201
    And the user "{{signupRejectName}}" in tenant "carbon.super" should exist

    When I capture the pending "AM_USER_SIGNUP" workflow reference where "tenantAwareUserName" is "{{signupRejectName}}" as "signupRejectWfRef"
    And I "REJECTED" the workflow with reference "signupRejectWfRef"
    Then The response status code should be 200
    And the user "{{signupRejectName}}" in tenant "carbon.super" should not exist
