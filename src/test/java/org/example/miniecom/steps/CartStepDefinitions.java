package org.example.miniecom.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.dto.request.CreateOrderItemRequest;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.dto.request.UpdateOrderRequest;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.example.miniecom.order.service.OrderService;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.dto.request.CreateProductRequest;
import org.example.miniecom.product.repository.ProductRepository;
import org.example.miniecom.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CartStepDefinitions {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private final Map<String, Long> productIdsByName = new HashMap<>();
    private Order createdOrder;
    private RuntimeException lastException;

    @Before
    public void resetDatabase() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        productIdsByName.clear();
        createdOrder = null;
        lastException = null;
    }

    @Given("the following products exist:")
    public void theFollowingProductsExist(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            Product created = productService.createProduct(new CreateProductRequest(
                    row.get("name"),
                    new BigDecimal(row.get("price")),
                    Integer.parseInt(row.get("stock"))
            ));
            productIdsByName.put(created.getName(), created.getId());
        }
    }

    @Given("an existing order for user {long} with:")
    public void anExistingOrderForUserWith(Long userId, DataTable dataTable) {
        createdOrder = orderService.createOrder(new CreateOrderRequest(userId, toOrderItems(dataTable)));
        lastException = null;
    }

    @When("user {long} adds products to the cart:")
    public void userAddsProductsToTheCart(Long userId, DataTable dataTable) {
        createdOrder = orderService.createOrder(new CreateOrderRequest(userId, toOrderItems(dataTable)));
        lastException = null;
    }

    @When("user {long} attempts to add products to the cart:")
    public void userAttemptsToAddProductsToTheCart(Long userId, DataTable dataTable) {
        createdOrder = null;
        try {
            orderService.createOrder(new CreateOrderRequest(userId, toOrderItems(dataTable)));
            lastException = null;
        } catch (RuntimeException ex) {
            lastException = ex;
        }
    }

    @When("the order is updated for user {long} with:")
    public void theOrderIsUpdatedForUserWith(Long userId, DataTable dataTable) {
        createdOrder = orderService.updateOrder(
                createdOrder.getId(),
                new UpdateOrderRequest(userId, toOrderItems(dataTable))
        );
        lastException = null;
    }

    @When("the order is deleted")
    public void theOrderIsDeleted() {
        orderService.deleteOrder(createdOrder.getId());
    }

    @Then("the cart total should be {bigdecimal}")
    public void theCartTotalShouldBe(BigDecimal expectedTotal) {
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Then("order creation should fail with message {string}")
    public void orderCreationShouldFailWithMessage(String expectedMessage) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).isEqualTo(expectedMessage);
    }

    @Then("order creation should fail with message containing {string}")
    public void orderCreationShouldFailWithMessageContaining(String expectedFragment) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).contains(expectedFragment);
    }

    @Then("product {string} stock should be {int}")
    public void productStockShouldBe(String productName, int expectedStock) {
        Long productId = productIdsByName.get(productName);
        Product product = productRepository.findById(productId).orElseThrow();
        assertThat(product.getStock()).isEqualTo(expectedStock);
    }

    @Then("total orders should be {int}")
    public void totalOrdersShouldBe(int expectedCount) {
        assertThat(orderRepository.count()).isEqualTo(expectedCount);
    }

    @Then("the order should not exist anymore")
    public void theOrderShouldNotExistAnymore() {
        assertThat(orderRepository.findById(createdOrder.getId())).isEmpty();
    }

    @And("the cart should contain {int} line items")
    public void theCartShouldContainLineItems(int expectedItems) {
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getItems()).hasSize(expectedItems);
    }

    private List<CreateOrderItemRequest> toOrderItems(DataTable dataTable) {
        return dataTable.asMaps(String.class, String.class).stream()
                .map(row -> {
                    String productName = row.get("name");
                    Long productId = productIdsByName.get(productName);
                    if (productId == null) {
                        throw new IllegalArgumentException("Unknown product name: " + productName);
                    }
                    return new CreateOrderItemRequest(productId, Integer.parseInt(row.get("quantity")));
                })
                .toList();
    }
}
