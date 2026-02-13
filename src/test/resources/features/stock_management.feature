@regression
Feature: Stock validation and reservation

  Scenario: Reject cart when stock is insufficient
    Given the following products exist:
      | name       | price | stock |
      | Headphones | 9.99  | 1     |
    When user 7 attempts to add products to the cart:
      | name       | quantity |
      | Headphones | 2        |
    Then order creation should fail with message containing "Insufficient stock for productId="
    And product "Headphones" stock should be 1
    And total orders should be 0

  @smoke
  Scenario: Reserve stock after successful cart creation
    Given the following products exist:
      | name       | price | stock |
      | Cable      | 15.00 | 10    |
    When user 11 adds products to the cart:
      | name  | quantity |
      | Cable | 3        |
    Then the cart total should be 45.00
    And product "Cable" stock should be 7
    And total orders should be 1
