@db #@disabled
Feature: TransactionOutboxConfig
  Passing properties to TransactionOutbox.

  Scenario: TransactionOutbox is configured with custom properties
    Given transaction outbox properties are configured with:
      | useJackson | unblockBlockedTasks | blockAfterAttempts | backgroundJobInitialDelay | backgroundJobRestartDelay | perTaskRetryDelay |
      | true       | true                | 5                  | PT1M                      | PT5M                      | PT30S             |
    When transaction outbox is created
    Then no exception should be thrown
