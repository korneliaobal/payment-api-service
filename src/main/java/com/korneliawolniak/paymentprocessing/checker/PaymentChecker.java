package com.korneliawolniak.paymentprocessing.checker;

import com.korneliawolniak.paymentprocessing.avro.PaymentValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.PaymentValidationResult;
import com.korneliawolniak.paymentprocessing.kafka.PaymentValidationResultPublisher;
import com.korneliawolniak.paymentprocessing.orchestrator.PaymentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentChecker {

  private final PaymentValidationResultPublisher paymentValidationResultPublisher;
  private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

  public PaymentChecker(PaymentValidationResultPublisher paymentValidationResultPublisher) {
    this.paymentValidationResultPublisher = paymentValidationResultPublisher;
  }

  @KafkaListener(topics = "payment-validation-request", groupId = "payment-checker")
  public void handle(PaymentValidationRequest request) {
    boolean valid = isValid(request);

    String status = valid ? "OK" : "NOT_OK";

    PaymentValidationResult result =
        PaymentValidationResult.newBuilder()
            .setPaymentId(request.getPaymentId())
            .setStatus(status)
            .build();

    paymentValidationResultPublisher.publish(result);

    log.info("Payment {} validation result: {}", request.getPaymentId(), status);
  }

  private boolean isValid(PaymentValidationRequest request) {
    return hasValidDebtorName(request)
        && hasValidDebtorAccountNumber(request)
        && hasValidTransactionCount(request);
  }

  private boolean hasValidDebtorName(PaymentValidationRequest request) {
    return request.getDebtorName() != null && !request.getDebtorName().toString().isBlank();
  }

  private boolean hasValidDebtorAccountNumber(PaymentValidationRequest request) {
    if (request.getDebtorAccountNumber() == null) {
      return false;
    }

    String accountNumber = request.getDebtorAccountNumber().toString();

    return accountNumber.length() == 28;
  }

  private boolean hasValidTransactionCount(PaymentValidationRequest request) {
    return request.getTransactionCount() <= 5;
  }
}
