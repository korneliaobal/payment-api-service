package com.korneliawolniak.paymentprocessing.checker;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import com.korneliawolniak.paymentprocessing.kafka.TransactionValidationResultPublisher;
import com.korneliawolniak.paymentprocessing.orchestrator.PaymentOrchestrator;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionChecker {
  private final TransactionValidationResultPublisher transactionValidationResultPublisher;
  private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

  public TransactionChecker(
      TransactionValidationResultPublisher transactionValidationResultPublisher) {
    this.transactionValidationResultPublisher = transactionValidationResultPublisher;
  }

  @KafkaListener(topics = "transaction-validation-request", groupId = "transaction-checker")
  public void handle(TransactionValidationRequest request) {
    boolean valid = isValid(request);

    String status = valid ? "OK" : "NOT_OK";

    TransactionValidationResult result =
        TransactionValidationResult.newBuilder()
            .setTransactionId(request.getTransactionId())
            .setPaymentId(request.getPaymentId())
            .setStatus(status)
            .build();

    transactionValidationResultPublisher.publish(result);

    log.info("Transaction {} validation result: {}", request.getTransactionId(), status);
  }

  private boolean isValid(TransactionValidationRequest request) {
    return hasValidCreditorName(request)
        && hasValidCreditorAccountNumber(request)
        && hasValidAmount(request);
  }

  private boolean hasValidCreditorName(TransactionValidationRequest request) {
    return request.getCreditorName() != null && !request.getCreditorName().toString().isBlank();
  }

  private boolean hasValidCreditorAccountNumber(TransactionValidationRequest request) {
    if (request.getCreditorAccountNumber() == null) {
      return false;
    }

    return request.getCreditorAccountNumber().toString().length() == 28;
  }

  private boolean hasValidAmount(TransactionValidationRequest request) {
    if (request.getAmount() == null) {
      return false;
    }

    BigDecimal amount = new BigDecimal(request.getAmount().toString());

    return amount.compareTo(BigDecimal.valueOf(5)) > 0;
  }
}
