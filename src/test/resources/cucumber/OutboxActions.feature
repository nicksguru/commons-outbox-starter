@db #@disabled
Feature: Outbox Actions
  OutboxActions stores actions in TransactionOutbox and performs them.
  All public methods must be transactional-  they join the caller's transaction by design.

  Background:
    Given an OutboxActions implementation is available

  Scenario: Creating an outbox proxy for a target class
    When an outbox proxy is created for the target class
    Then the Outbox proxy should not be null
    And the Outbox proxy should be an instance of the target class

  Scenario: Outbox proxy executes scheduled actions
    When an action is scheduled through the outbox proxy
    Then the outboxed action should be executed successfully

  Scenario: Outbox handles transaction commit
    Given a transaction is started
    When an action is scheduled through the outbox proxy
    And the transaction is committed
    Then the outboxed action should be executed successfully

  Scenario: Outbox handles transaction rollback
    Given a transaction is started
    When an action is scheduled through the outbox proxy
    And the transaction is rolled back
    Then the outboxed action should not be executed

  Scenario Outline: Outbox handles different types of actions
    When an action of type "<actionType>" with parameter "<parameter>" is scheduled
    Then the outboxed action should be executed with the correct parameter
    Examples:
      | actionType | parameter     |
      | simple     | testParameter |
      | complex    | {\"id\": 123} |
      | empty      |               |
