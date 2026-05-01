Feature: Migrated API Revision Management

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario Outline: Migrated API Revision Management
    # Step 1: Find the api
    When I find the API created with the name "<apiName>" and version "<apiVersion>"
    And I wait until the response status code is 200 and the value of response field "count" is "1"
    And I extract response field "list[0].id" and store it as "<apiID>"

    # Step 2: Verify the availability of  existing revisions
    When I get the existing revision for "apis" resource with "<apiID>"
    And I wait until the response status code is 200
    And I extract response field "list[0].id" and store it as "<existingRevisionID>"

    # Step 3: Create a new revision of an existing API and save in context as "revisionId"
    When I put the following JSON payload in context as "<createRevisionPayload>"
    """
    {
      "description":"Another new Revision"
    }
    """
    And I make a request to create a revision for "apis" resource "<apiID>" with payload "<createRevisionPayload>"
    And I wait until the response status code is 201
    And I extract response field "id" and store it as "<revisionId>"
    And I wait for 3 seconds

    # At this point, we have newly created revision as "revisionId" and already existing first revision as "existingRevisionID"

    # Step 4: Deploy a specific revision to a gateway environment
    When I deploy revision "revisionId" of "apis" resource "<apiID>"
    And I wait until the response status code is 201

    # Step 5: Undeploy an existing API revision
    When I undeploy revision "revisionId" of "apis" resource "<apiID>"
    And I wait until the response status code is 201

    # Step 6: Restore a previous revision to make it the current working copy
    When I restore a previous revision "revisionId" of "apis" resource "<apiID>"
    And I wait until the response status code is 201

    # Step 7: Delete an unused or old revision, created revision
    When I Delete the "apis" resource revision with "revisionId" for "<apiID>"
    And I wait until the response status code is 200

    Examples:
     | apiName                  | apiVersion   | apiID         |
     | ADPRestAPI               | 1.0.0        | RestApiId     |
     | ADPStarWarsAPI           | 1.0.0        | GraphQLApiId  |
     | ADPPhoneVerificationAPI  | 1.0.0        | SoapApiId     |
     | ADPIfElseAPI             | 1.0.0        | AsyncApiId    |
