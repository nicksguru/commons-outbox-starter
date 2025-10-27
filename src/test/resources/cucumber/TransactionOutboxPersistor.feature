@db #@disabled
Feature: TransactionOutbox persistor configuration

  Scenario: Persistor is configured without Jackson serialization
    Given transaction outbox properties with Jackson serialization false
    When a persistor is created
    Then the persistor should be properly configured

  Scenario: Persistor is configured with Jackson serialization
    Given transaction outbox properties with Jackson serialization true
    When a persistor is created
    Then the persistor should be properly configured
    And the persistor should use Jackson serialization
