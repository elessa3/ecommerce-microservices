package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.event.OrderEvent;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository       orderRepository;
    private final OrderMapper           orderMapper;
    private final OrderNumberGenerator  orderNumberGenerator;
    private final ProductClient         productClient;
    private final PaymentService        paymentService;
    private final OrderEventPublisher   eventPublisher;

    // ── Create order (PENDING) ────────────────────────────────────

    @Transactional
    public OrderDto.CreateResponse createOrder(OrderDto.CreateRequest request) {

        // 1. Validate products and build line items by calling product-service.
        //    Wrapped in a circuit breaker (see ProductClient) so a slow/down
        //    product-service fails fast instead of hanging this request.
        List<OrderItem> orderItems = request.getItems().stream()
            .map(itemReq -> {
                ProductClient.ProductInfo product = productClient.getProduct(itemReq.getProductId());
                return OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .unitPrice(product.price())
                    .quantity(itemReq.getQuantity())
                    .build();
            })
            .toList();

        BigDecimal total = orderItems.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Persist the order in PENDING state
        Order order = Order.builder()
            .orderNumber(orderNumberGenerator.generate())
            .customerId(request.getCustomerId())
            .status(OrderStatus.PENDING)
            .totalAmount(total)
            .currency("EUR")
            .shippingAddress(request.getShippingAddress())
            .build();

        orderItems.forEach(order::addItem);
        Order saved = orderRepository.save(order);

        // 3. Create the Stripe PaymentIntent — frontend completes payment client-side
        PaymentService.PaymentIntentResult paymentResult =
            paymentService.createPaymentIntent(total, "EUR", saved.getOrderNumber());

        saved.setPaymentIntentId(paymentResult.paymentIntentId());
        orderRepository.save(saved);

        // 4. Publish OrderPlaced event — notification-service will send a
        //    "we received your order" email asynchronously
        eventPublisher.publish(toOrderPlacedEvent(saved));

        log.info("Order created: {} for customer {}", saved.getOrderNumber(), saved.getCustomerId());

        return OrderDto.CreateResponse.builder()
            .order(orderMapper.toResponse(saved))
            .paymentClientSecret(paymentResult.clientSecret())
            .build();
    }

    // ── Confirm order (called by Stripe webhook after payment succeeds) ──

    @Transactional
    public void confirmOrder(String paymentIntentId) {
        Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No order found for payment intent: " + paymentIntentId));

        order.transitionTo(OrderStatus.CONFIRMED);   // throws if already confirmed/cancelled
        orderRepository.save(order);

        eventPublisher.publish(OrderEvent.OrderConfirmed.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .paymentIntentId(paymentIntentId)
            .occurredAt(LocalDateTime.now())
            .build());

        log.info("Order confirmed: {}", order.getOrderNumber());
    }

    // ── Ship order ─────────────────────────────────────────────────

    @Transactional
    public OrderDto.Response shipOrder(Long orderId, String trackingNumber) {
        Order order = findOrderOrThrow(orderId);
        order.transitionTo(OrderStatus.SHIPPED);
        Order saved = orderRepository.save(order);

        eventPublisher.publish(OrderEvent.OrderShipped.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .trackingNumber(trackingNumber)
            .occurredAt(LocalDateTime.now())
            .build());

        return orderMapper.toResponse(saved);
    }

    // ── Cancel order ───────────────────────────────────────────────

    @Transactional
    public OrderDto.Response cancelOrder(Long orderId, String reason) {
        Order order = findOrderOrThrow(orderId);
        order.transitionTo(OrderStatus.CANCELLED);   // throws if order is already SHIPPED/DELIVERED
        Order saved = orderRepository.save(order);

        eventPublisher.publish(OrderEvent.OrderCancelled.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .reason(reason)
            .occurredAt(LocalDateTime.now())
            .build());

        log.info("Order cancelled: {} — reason: {}", order.getOrderNumber(), reason);
        return orderMapper.toResponse(saved);
    }

    // ── Read operations ───────────────────────────────────────────

    public OrderDto.Response findById(Long id) {
        return orderMapper.toResponse(findOrderOrThrow(id));
    }

    // ── Helpers ────────────────────────────────────────────────────

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findByIdWithItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private OrderEvent.OrderPlaced toOrderPlacedEvent(Order order) {
        List<OrderEvent.ItemSnapshot> items = order.getItems().stream()
            .map(item -> OrderEvent.ItemSnapshot.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build())
            .toList();

        return OrderEvent.OrderPlaced.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .items(items)
            .occurredAt(LocalDateTime.now())
            .build();
    }
}
