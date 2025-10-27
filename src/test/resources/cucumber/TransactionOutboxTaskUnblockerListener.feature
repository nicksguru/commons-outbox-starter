@db #@disabled
Feature: TransactionOutboxTaskUnblockerListener
  Test unblocking blocked tasks (for eternal retries) if so was specified in config.

  Scenario Outline: Transaction outbox task blocked event is processed
    Given transaction outbox properties with unblock blocked tasks set to <unblockBlockedTasks>
    When a transaction outbox task blocked event is received
    Then the task should <action>
    Examples:
      | unblockBlockedTasks | action         |
      | true                | be unblocked   |
      | false               | remain blocked |
