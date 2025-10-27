@db #@disabled
Feature: TransactionOutboxBackgroundJob
  Retry outboxed tasks.

  Background:
    Given the transaction outbox background job is initialized
    And the initial delay is set to "5 minutes"
    And the restart delay is set to "10 minutes"

  Scenario: Background job successfully processes tasks
    When there are tasks to process
    And the background job is set up
    And the background job retries failed tasks
    Then all tasks should be processed
    And no exception should be thrown

  Scenario: Background job hides errors from outboxes actions (the point is to retry them periodically)
    When the transaction outbox is set up to throw an exception
    When the background job is set up
    And the background job retries failed tasks
    Then no exception should be thrown
