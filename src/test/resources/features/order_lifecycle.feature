@regression
Feature: Order lifecycle management

  Scenario: Update order recalculates total and reconciles stock
    Given the following products exist:
      | name       | price | stock |
      | Mouse      | 20.00 | 10    |
      | Keyboard   | 50.00 | 10    |
    And an existing order for user 5 with:
      | name  | quantity |
      | Mouse | 2        |
    When the order is updated for user 9 with:
      | name     | quantity |
      | Keyboard | 1        |
    Then the cart total should be 50.00
    And the cart should contain 1 line items
    And product "Mouse" stock should be 10
    And product "Keyboard" stock should be 9

  Scenario: Delete order restores reserved stock
    Given the following products exist:
      | name       | price | stock |
      | Dock       | 30.00 | 5     |
    And an existing order for user 2 with:
      | name | quantity |
      | Dock | 2        |
    When the order is deleted
    Then the order should not exist anymore
    And product "Dock" stock should be 5
    And total orders should be 0
