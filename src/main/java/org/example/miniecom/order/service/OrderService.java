package org.example.miniecom.order.service;

import lombok.RequiredArgsConstructor;
import org.example.miniecom.order.dto.request.CreateOrderItemRequest;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.dto.request.UpdateOrderRequest;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderItem;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validateItemsNotEmpty(request.items());
        Map<Long, Integer> requestedQtyByProductId = aggregateRequestedQuantities(request.items());
        Map<Long, Product> productsById = fetchAndValidateProducts(requestedQtyByProductId);

        Order order = Order.builder()
                .userId(request.userId())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = rebuildOrderItems(order, request.items(), productsById);

        applyStockAdjustments(productsById, requestedQtyByProductId);

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateOrder(Long id, UpdateOrderRequest request) {
        validateItemsNotEmpty(request.items());

        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Map<Long, Integer> existingQtyByProductId = aggregateExistingQuantities(order.getItems());
        Map<Long, Integer> requestedQtyByProductId = aggregateRequestedQuantities(request.items());
        Map<Long, Integer> stockDeltasByProductId = new HashMap<>();
        for (Long productId : existingQtyByProductId.keySet()) {
            stockDeltasByProductId.put(productId, existingQtyByProductId.get(productId));
        }
        for (Long productId : requestedQtyByProductId.keySet()) {
            stockDeltasByProductId.merge(productId, -requestedQtyByProductId.get(productId), Integer::sum);
        }

        Map<Long, Product> productsById = fetchAndValidateProductsForUpdate(
                stockDeltasByProductId.keySet(),
                requestedQtyByProductId,
                existingQtyByProductId
        );
        applyStockDeltas(productsById, stockDeltasByProductId);

        order.setUserId(request.userId());
        order.getItems().clear();
        BigDecimal total = rebuildOrderItems(order, request.items(), productsById);
        order.setTotalAmount(total);

        productRepository.saveAll(productsById.values());
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Map<Long, Integer> existingQtyByProductId = aggregateExistingQuantities(order.getItems());
        Map<Long, Product> productsById = fetchProducts(existingQtyByProductId.keySet());

        for (Map.Entry<Long, Integer> entry : existingQtyByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.setStock(product.getStock() + entry.getValue());
        }

        productRepository.saveAll(productsById.values());
        orderRepository.delete(order);
    }

    private Map<Long, Integer> aggregateRequestedQuantities(List<CreateOrderItemRequest> items) {
        Map<Long, Integer> requestedQtyByProductId = new HashMap<>();
        for (CreateOrderItemRequest item : items) {
            requestedQtyByProductId.merge(item.productId(), item.amount(), Integer::sum);
        }
        return requestedQtyByProductId;
    }

    private Map<Long, Integer> aggregateExistingQuantities(List<OrderItem> items) {
        Map<Long, Integer> existingQtyByProductId = new HashMap<>();
        for (OrderItem item : items) {
            existingQtyByProductId.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return existingQtyByProductId;
    }

    private void validateItemsNotEmpty(List<CreateOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderItemsException();
        }
    }

    private BigDecimal rebuildOrderItems(
            Order order,
            List<CreateOrderItemRequest> items,
            Map<Long, Product> productsById
    ) {
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : items) {
            Product product = productsById.get(itemRequest.productId());
            BigDecimal unitPrice = product.getPrice();
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.amount())
                    .price(unitPrice)
                    .build();
            order.addItem(item);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemRequest.amount())));
        }
        return total;
    }

    private void applyStockAdjustments(Map<Long, Product> productsById, Map<Long, Integer> requestedQtyByProductId) {
        for (Map.Entry<Long, Integer> entry : requestedQtyByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.setStock(product.getStock() - entry.getValue());
        }
        productRepository.saveAll(productsById.values());
    }

    private Map<Long, Product> fetchAndValidateProducts(Map<Long, Integer> requestedQtyByProductId) {
        Map<Long, Product> productsById = fetchProducts(requestedQtyByProductId.keySet());
        for (Map.Entry<Long, Integer> entry : requestedQtyByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            if (product.getStock() < entry.getValue()) {
                throw new InsufficientStockException(entry.getKey());
            }
        }
        return productsById;
    }

    private Map<Long, Product> fetchAndValidateProductsForUpdate(
            Iterable<Long> productIds,
            Map<Long, Integer> requestedQtyByProductId,
            Map<Long, Integer> existingQtyByProductId
    ) {
        Map<Long, Product> fetchedProductsById = fetchProducts(productIds);
        for (Map.Entry<Long, Integer> entry : requestedQtyByProductId.entrySet()) {
            Long productId = entry.getKey();
            int requestedQuantity = entry.getValue();
            int alreadyReservedByOrder = existingQtyByProductId.getOrDefault(productId, 0);
            int availableQuantity = fetchedProductsById.get(productId).getStock() + alreadyReservedByOrder;
            if (requestedQuantity > availableQuantity) {
                throw new InsufficientStockException(productId);
            }
        }
        return fetchedProductsById;
    }

    private void applyStockDeltas(Map<Long, Product> productsById, Map<Long, Integer> stockDeltasByProductId) {
        for (Map.Entry<Long, Integer> entry : stockDeltasByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            product.setStock(product.getStock() + entry.getValue());
        }
    }

    private Map<Long, Product> fetchProducts(Iterable<Long> productIds) {
        Map<Long, Product> productsById = new HashMap<>();
        for (Long productId : productIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new InvalidOrderProductException(productId));
            productsById.put(productId, product);
        }
        return productsById;
    }
}
