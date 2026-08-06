@remote-logging
Feature: Remote Server Logging

  Server-global remote logging (RemoteLoggingAppenderTest): the Carbon RemoteLoggingConfig admin service
  redirects a log type's appender to a remote HTTP endpoint. Enabling it rewrites that log type's appender in
  the running server's log4j2.properties from a local RollingFile to a SecuredHttp appender AND streams the log
  entries to the remote endpoint; disabling it reverts the appender and stops the stream.

  Beyond the end-to-end arc, the scenarios below pin how the service MANAGES those appender blocks, seeding a
  log4j2.properties fixture (artifacts/configFiles/remoteLogging/) whose blocks have been stripped: a missing
  block must be created as a remote appender AND registered in the top-level appenders list (an unlisted
  appender is one log4j2 never instantiates), an already-listed appender must not be duplicated, and appenders
  for log types that have no remote URL must be left exactly as they are — including by the startup-time
  syncRemoteServerConfigs reconciliation.

  Remote logging is a super-tenant, server-wide setting, so these run once as the super-tenant admin in a
  dedicated thread-count=1 block (they mutate the shared server's log configuration). The teardown hook resets
  every log type the scenario enabled and writes the pristine log4j2.properties back, so a stripped fixture
  never leaks into the next scenario.

  Background:
    Given The system is ready
    And I have valid access tokens as "admin"
    # A host sink the container reaches via host.docker.internal. Every scenario points its remote appender here:
    # a live receiver keeps the appender from retrying against a dead endpoint while the file is being asserted.
    And I start a mock log sink and store its container URL as "sinkUrl"

  @cap:analytics @feat:remote-logging @rule:end-to-end @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Audit logs stream to a remote endpoint when enabled and stop when disabled
    # Start from the all-appenders fixture so the assertions below hold whatever an earlier scenario left behind
    Given I apply the log4j2 fixture "log4j2WithAllAppenders.properties" to the server
    # Enable remote logging for the AUDIT log type, pointing at the sink
    When I enable remote logging for log type "AUDIT" pointing at URL "{{sinkUrl}}"
    Then The response status code should be 202
    # The AUDIT_LOGFILE appender is rewritten to the HTTP (SecuredHttp) type in log4j2.properties
    And the "AUDIT_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    # Enabling one log type must not disturb the appenders of the log types that were not targeted
    And the "CARBON_LOGFILE" log appender type should be "RollingFile"
    And the "API_LOGFILE" log appender type should be "RollingFile"
    # An audit-producing admin action's log entry reaches the remote sink
    When I trigger an audit log entry
    Then the mock log sink should receive a log payload within 30 seconds
    # Disabling reverts the appender to the local RollingFile and stops the remote stream
    When I disable remote logging for log type "AUDIT"
    Then The response status code should be 202
    And the "AUDIT_LOGFILE" log appender should become "RollingFile" within 30 seconds
    And the "CARBON_LOGFILE" log appender type should be "RollingFile"
    And the "API_LOGFILE" log appender type should be "RollingFile"
    And the mock log sink should stop receiving payloads within 30 seconds

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Resetting remote logging with a blank URL leaves the existing local appender blocks untouched
    Given I apply the log4j2 fixture "log4j2WithAllAppenders.properties" to the server
    And the "AUDIT_LOGFILE" log appender type should be "RollingFile"
    And the "CARBON_LOGFILE" log appender type should be "RollingFile"
    And the "API_LOGFILE" log appender type should be "RollingFile"
    # A reset carries a blank URL, so the appenders are already in their default local-file state — every one of
    # these calls must be a no-op rather than a rewrite of the block
    When I disable remote logging for log type "AUDIT"
    And I disable remote logging for log type "CARBON"
    And I disable remote logging for log type "API"
    Then the "AUDIT_LOGFILE" log appender type should remain "RollingFile" for 10 seconds
    And the "CARBON_LOGFILE" log appender type should be "RollingFile"
    And the "API_LOGFILE" log appender type should be "RollingFile"

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario Outline: A missing <appender> block is created and listed when remote logging is enabled for <logType>
    Given I apply the log4j2 fixture "<fixture>" to the server
    And the "<appender>" log appender block should be absent
    When I enable remote logging for log type "<logType>" pointing at URL "{{sinkUrl}}"
    Then The response status code should be 202
    And the "<appender>" log appender should become "SecuredHttp" within 30 seconds
    And the "<appender>" log appender block should define property "url"
    And the "<appender>" log appender should be listed in the appenders list exactly 1 time

    Examples:
      | logType | appender       | fixture                                |
      | AUDIT   | AUDIT_LOGFILE  | log4j2WithoutAuditAppender.properties  |
      | CARBON  | CARBON_LOGFILE | log4j2WithoutCarbonAppender.properties |
      | API     | API_LOGFILE    | log4j2WithoutApiAppender.properties    |

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario Outline: Sync recreates the <appender> block when it is removed from a file that still configures <logType>
    Given I apply the log4j2 fixture "log4j2WithAllAppenders.properties" to the server
    When I enable remote logging for log type "<logType>" pointing at URL "{{sinkUrl}}"
    Then the "<appender>" log appender should become "SecuredHttp" within 30 seconds
    And the "<otherAppender1>" log appender type should be "RollingFile"
    And the "<otherAppender2>" log appender type should be "RollingFile"
    # Swap in a file with no <appender> block while <logType> stays configured for remote logging, so sync sees a
    # mismatch between what is persisted and what is on disk
    When I apply the log4j2 fixture "<fixture>" to the server
    And I sync the remote logging configurations
    Then the "<appender>" log appender should become "SecuredHttp" within 30 seconds
    And the "<appender>" log appender should be listed in the appenders list exactly 1 time
    And the "<otherAppender1>" log appender type should be "RollingFile"
    And the "<otherAppender2>" log appender type should be "RollingFile"

    Examples:
      | logType | appender       | otherAppender1 | otherAppender2 | fixture                                |
      | AUDIT   | AUDIT_LOGFILE  | CARBON_LOGFILE | API_LOGFILE    | log4j2WithoutAuditAppender.properties  |
      | CARBON  | CARBON_LOGFILE | AUDIT_LOGFILE  | API_LOGFILE    | log4j2WithoutCarbonAppender.properties |
      | API     | API_LOGFILE    | AUDIT_LOGFILE  | CARBON_LOGFILE | log4j2WithoutApiAppender.properties    |

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Enabling remote logging for one log type creates only that appender when all three blocks are absent
    Given I apply the log4j2 fixture "log4j2WithoutLocalAppenders.properties" to the server
    And the "AUDIT_LOGFILE" log appender block should be absent
    And the "CARBON_LOGFILE" log appender block should be absent
    And the "API_LOGFILE" log appender block should be absent
    When I enable remote logging for log type "API" pointing at URL "{{sinkUrl}}"
    Then the "API_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    And the "API_LOGFILE" log appender should be listed in the appenders list exactly 1 time
    # Enabling remote logging is scoped to the log type it was called with — it must not implicitly create the
    # appender blocks of the other log types
    And the "AUDIT_LOGFILE" log appender block should be absent
    And the "CARBON_LOGFILE" log appender block should be absent
    And the "AUDIT_LOGFILE" log appender should be listed in the appenders list exactly 0 times
    And the "CARBON_LOGFILE" log appender should be listed in the appenders list exactly 0 times

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Sync recreates only the configured log type's appender when all three blocks are absent
    Given I apply the log4j2 fixture "log4j2WithoutLocalAppenders.properties" to the server
    When I enable remote logging for log type "API" pointing at URL "{{sinkUrl}}"
    Then the "API_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    # Reload the stripped fixture before syncing, so the API_LOGFILE block is absent from the file again. This
    # also keeps the URL comparison on the server short-circuiting before sync reaches the null-field checks:
    # syncing while the file already carries a matching URL trips a server-side NPE, because the stub data
    # returned by getRemoteServerConfigs carries no username and the comparison dereferences it.
    When I apply the log4j2 fixture "log4j2WithoutLocalAppenders.properties" to the server
    And I sync the remote logging configurations
    Then the "API_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    And the "API_LOGFILE" log appender should be listed in the appenders list exactly 1 time
    And the "AUDIT_LOGFILE" log appender block should remain absent for 10 seconds
    And the "CARBON_LOGFILE" log appender block should be absent
    And the "AUDIT_LOGFILE" log appender should be listed in the appenders list exactly 0 times
    And the "CARBON_LOGFILE" log appender should be listed in the appenders list exactly 0 times

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Sync leaves a stripped log4j2 configuration untouched when no remote logging is configured
    # Clear any persisted remote config first, so this asserts the unconfigured path and does not depend on what
    # an earlier scenario left in the registry. Done before the fixture is seeded, since a reset can rewrite the file.
    Given I disable remote logging for log type "AUDIT"
    And I disable remote logging for log type "CARBON"
    And I disable remote logging for log type "API"
    And I apply the log4j2 fixture "log4j2WithoutLocalAppenders.properties" to the server
    # Sync is the path the server takes at startup; with no remote URL configured it must not touch the file
    When I sync the remote logging configurations
    Then the "AUDIT_LOGFILE" log appender block should remain absent for 10 seconds
    And the "CARBON_LOGFILE" log appender block should be absent
    And the "API_LOGFILE" log appender block should be absent

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: All three missing appender blocks are created and listed when every log type enables remote logging
    Given I apply the log4j2 fixture "log4j2WithoutLocalAppenders.properties" to the server
    When I enable remote logging for log type "AUDIT" pointing at URL "{{sinkUrl}}"
    Then the "AUDIT_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    When I enable remote logging for log type "CARBON" pointing at URL "{{sinkUrl}}"
    Then the "CARBON_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    When I enable remote logging for log type "API" pointing at URL "{{sinkUrl}}"
    Then the "API_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    # Each write also has to register its appender, and must not drop the ones written before it
    And the "AUDIT_LOGFILE" log appender should be listed in the appenders list exactly 1 time
    And the "CARBON_LOGFILE" log appender should be listed in the appenders list exactly 1 time
    And the "API_LOGFILE" log appender should be listed in the appenders list exactly 1 time

  @cap:analytics @feat:remote-logging @rule:appender-management @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: An appender already in the appenders list is not duplicated when remote logging is toggled
    Given I apply the log4j2 fixture "log4j2WithAllAppenders.properties" to the server
    And the "AUDIT_LOGFILE" log appender should be listed in the appenders list exactly 1 time
    # Enable then reset, so the appenders list is rewritten twice for an appender that was already listed
    When I enable remote logging for log type "AUDIT" pointing at URL "{{sinkUrl}}"
    Then the "AUDIT_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    When I disable remote logging for log type "AUDIT"
    Then the "AUDIT_LOGFILE" log appender should become "RollingFile" within 30 seconds
    And the "AUDIT_LOGFILE" log appender should be listed in the appenders list exactly 1 time
