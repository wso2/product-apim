@remote-logging
Feature: Remote Server Logging

  Server-global remote logging (RemoteLoggingAppenderTest): the Carbon RemoteLoggingConfig admin service
  redirects a log type's appender to a remote HTTP endpoint. Enabling it flips the AUDIT_LOGFILE appender in
  the running server's log4j2.properties from a local RollingFile to a SecuredHttp appender AND streams audit
  log entries to the remote endpoint; disabling it reverts the appender and stops the stream. Remote logging is
  a super-tenant, server-wide setting, so this runs once as the super-tenant admin in a dedicated
  thread-count=1 block (it mutates the shared server's log configuration).

  @cap:analytics @feat:remote-logging @rule:end-to-end @type:regression @legacy:RemoteLoggingAppenderTest
  Scenario: Audit logs stream to a remote endpoint when enabled and stop when disabled
    Given The system is ready
    And I have valid access tokens as "admin"
    # Stand up a host sink the container reaches via host.docker.internal
    When I start a mock log sink and store its container URL as "sinkUrl"
    # Enable remote logging for the AUDIT log type, pointing at the sink
    When I enable remote logging for log type "AUDIT" pointing at URL "{{sinkUrl}}"
    Then The response status code should be 202
    # The AUDIT_LOGFILE appender is rewritten to the HTTP (SecuredHttp) type in log4j2.properties
    And the "AUDIT_LOGFILE" log appender should become "SecuredHttp" within 30 seconds
    # An audit-producing admin action's log entry reaches the remote sink
    When I trigger an audit log entry
    Then the mock log sink should receive a log payload within 30 seconds
    # Disabling reverts the appender to the local RollingFile and stops the remote stream
    When I disable remote logging for log type "AUDIT"
    Then The response status code should be 202
    And the "AUDIT_LOGFILE" log appender should become "RollingFile" within 30 seconds
    And the mock log sink should stop receiving payloads within 30 seconds
