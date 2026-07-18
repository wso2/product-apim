@setup
Feature: Setup subscribed application

  Creates an application, generates production keys, subscribes it to the published REST API, and
  obtains an access token. Asserts nothing; ids are registered for @After cleanup.

  Background:
    Given The system is ready and I have valid access tokens for current user

  Scenario: Subscribe an application to the published API
    When I have set up application with keys, subscribed to API "publishedApiId", and obtained access token for "subscriptionId"
