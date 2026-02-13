@regression
Feature: Cart pricing

  @smoke
  Scenario: Add product to cart and verify total
    Given the following products exist:
      | name       | price | stock |
      | Headphones | 9.99  | 10    |
      | Stand      | 5.00  | 10    |
    When user 7 adds products to the cart:
      | name       | quantity |
      | Headphones | 2        |
      | Stand      | 1        |
    Then the cart total should be 24.98
    And the cart should contain 2 line items
