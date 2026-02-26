package com.pingxin403.cuckoo.payment.service;

import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.payment.config.TestConfig;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.repository.PaymentRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for PaymentService
 *
 * Tests payment status transitions and data integrity.
 *
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@JqwikSpringSupport
@SpringBootTest(classes = com.pingxin403.cuckoo.payment.TestPaymentApplication.class)
@ActiveProfiles("test")
@Import(TestConfig.class)
class PaymentServicePropertyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    // ========== Helper Methods ==========

    private Payment createTestPayment(Long orderId, Long userId, int amountCents) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(BigDecimal.valueOf(amountCents, 2));
        return payment;
    }

    /**
     * Property: Payment creation sets initial status to PENDING
     *
     * For any valid payment data, creating a payment should set status to PENDING
     * and persist all fields correctly.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Payment creation sets initial status to PENDING")
    @Transactional
    void paymentCreation_setsInitialStatusToPending(
            @ForAll @Positive Long orderId,
            @ForAll @Positive Long userId,
            @ForAll @IntRange(min = 100, max = 999999) int amountCents) {

        paymentRepository.deleteAll();

        Payment payment = createTestPayment(orderId, userId, amountCents);
        Payment created = paymentService.createPayment(payment);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getOrderId()).isEqualTo(orderId);
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(amountCents, 2));
        assertThat(created.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
    }

    /**
     * Property: Payment confirmation transitions status from PENDING to SUCCESS
     *
     * For any created payment, confirming it should change status to SUCCESS.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Payment confirmation transitions status to SUCCESS")
    @Transactional
    void paymentConfirmation_transitionsToSuccess(
            @ForAll @Positive Long orderId,
            @ForAll @Positive Long userId,
            @ForAll @IntRange(min = 100, max = 999999) int amountCents) {

        paymentRepository.deleteAll();

        Payment payment = createTestPayment(orderId, userId, amountCents);
        Payment created = paymentService.createPayment(payment);

        Payment confirmed = paymentService.confirmPayment(created.getId());

        assertThat(confirmed.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
        assertThat(confirmed.getId()).isEqualTo(created.getId());
        assertThat(confirmed.getOrderId()).isEqualTo(orderId);
        assertThat(confirmed.getAmount()).isEqualByComparingTo(created.getAmount());
    }

    /**
     * Property: Payment failure transitions status from PENDING to FAILED
     *
     * For any created payment, failing it should change status to FAILED.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Payment failure transitions status to FAILED")
    @Transactional
    void paymentFailure_transitionsToFailed(
            @ForAll @Positive Long orderId,
            @ForAll @Positive Long userId,
            @ForAll @IntRange(min = 100, max = 999999) int amountCents) {

        paymentRepository.deleteAll();

        Payment payment = createTestPayment(orderId, userId, amountCents);
        Payment created = paymentService.createPayment(payment);

        Payment failed = paymentService.failPayment(created.getId(), "Insufficient funds");

        assertThat(failed.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
        assertThat(failed.getId()).isEqualTo(created.getId());
        assertThat(failed.getOrderId()).isEqualTo(orderId);
    }

    /**
     * Property: Payment query returns correct payment information
     *
     * For any created payment, querying by ID should return the same data.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Payment query returns correct payment information")
    @Transactional
    void paymentQuery_returnsCorrectInformation(
            @ForAll @Positive Long orderId,
            @ForAll @Positive Long userId,
            @ForAll @IntRange(min = 100, max = 999999) int amountCents) {

        paymentRepository.deleteAll();

        Payment payment = createTestPayment(orderId, userId, amountCents);
        Payment created = paymentService.createPayment(payment);

        Payment queried = paymentService.getPaymentById(created.getId());

        assertThat(queried.getId()).isEqualTo(created.getId());
        assertThat(queried.getOrderId()).isEqualTo(created.getOrderId());
        assertThat(queried.getUserId()).isEqualTo(created.getUserId());
        assertThat(queried.getAmount()).isEqualByComparingTo(created.getAmount());
        assertThat(queried.getStatus()).isEqualTo(created.getStatus());
    }

    /**
     * Property: Query for non-existent payment throws ResourceNotFoundException
     *
     * For any ID that doesn't exist in the database, querying should throw.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Payment query throws exception for non-existent payment")
    @Transactional
    void paymentQuery_throwsExceptionForNonExistentPayment(
            @ForAll("positiveIds") Long nonExistentId) {

        paymentRepository.deleteAll();

        Assume.that(!paymentRepository.existsById(nonExistentId));

        assertThatThrownBy(() -> paymentService.getPaymentById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Property: Full payment lifecycle PENDING -> SUCCESS preserves data integrity
     *
     * For any payment, the full create-confirm lifecycle should preserve
     * all payment data while only changing the status.
     *
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Full payment lifecycle preserves data integrity")
    @Transactional
    void paymentLifecycle_preservesDataIntegrity(
            @ForAll @Positive Long orderId,
            @ForAll @Positive Long userId,
            @ForAll @IntRange(min = 100, max = 999999) int amountCents) {

        paymentRepository.deleteAll();

        // Create
        Payment payment = createTestPayment(orderId, userId, amountCents);
        Payment created = paymentService.createPayment(payment);
        assertThat(created.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);

        // Confirm
        Payment confirmed = paymentService.confirmPayment(created.getId());
        assertThat(confirmed.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);

        // Verify data integrity - all fields except status should be unchanged
        assertThat(confirmed.getId()).isEqualTo(created.getId());
        assertThat(confirmed.getOrderId()).isEqualTo(created.getOrderId());
        assertThat(confirmed.getUserId()).isEqualTo(created.getUserId());
        assertThat(confirmed.getAmount()).isEqualByComparingTo(created.getAmount());
    }

    // ========== Data Generators ==========

    /**
     * Generate positive IDs for non-existent payment queries
     */
    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 999999L);
    }
}
