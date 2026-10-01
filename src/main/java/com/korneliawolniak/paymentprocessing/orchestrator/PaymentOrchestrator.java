package com.korneliawolniak.paymentprocessing.orchestrator;

import com.korneliawolniak.paymentprocessing.avro.PaymentCreatedEvent;
import com.korneliawolniak.paymentprocessing.avro.PaymentValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.PaymentValidationResult;
import com.korneliawolniak.paymentprocessing.avro.TransactionEvent;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import com.korneliawolniak.paymentprocessing.kafka.PaymentValidationRequestPublisher;
import com.korneliawolniak.paymentprocessing.kafka.TransactionValidationRequestPublisher;
import com.korneliawolniak.paymentprocessing.mapper.PaymentValidationRequestMapper;
import com.korneliawolniak.paymentprocessing.mapper.TransactionValidationRequestMapper;
import com.korneliawolniak.paymentprocessing.persistence.PaymentEntity;
import com.korneliawolniak.paymentprocessing.persistence.PaymentRepository;
import com.korneliawolniak.paymentprocessing.persistence.PaymentStatus;
import com.korneliawolniak.paymentprocessing.persistence.TransactionEntity;
import com.korneliawolniak.paymentprocessing.persistence.TransactionRepository;
import com.korneliawolniak.paymentprocessing.service.PaymentStatusAggregator;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrchestrator {

  private final PaymentRepository paymentRepository;
  private final TransactionRepository transactionRepository;
  private final PaymentValidationRequestMapper paymentValidationRequestMapper;
  private final PaymentValidationRequestPublisher paymentValidationRequestPublisher;
  private final TransactionValidationRequestMapper transactionValidationRequestMapper;
  private final TransactionValidationRequestPublisher transactionValidationRequestPublisher;
  private final PaymentStatusAggregator paymentStatusAggregator;

  public PaymentOrchestrator(
      PaymentRepository paymentRepository,
      TransactionRepository transactionRepository,
      PaymentValidationRequestMapper paymentValidationRequestMapper,
      PaymentValidationRequestPublisher paymentValidationRequestPublisher,
      TransactionValidationRequestMapper transactionValidationRequestMapper,
      TransactionValidationRequestPublisher transactionValidationRequestPublisher,
      PaymentStatusAggregator paymentStatusAggregator) {
    this.paymentRepository = paymentRepository;
    this.transactionRepository = transactionRepository;
    this.paymentValidationRequestMapper = paymentValidationRequestMapper;
    this.paymentValidationRequestPublisher = paymentValidationRequestPublisher;
    this.transactionValidationRequestMapper = transactionValidationRequestMapper;
    this.transactionValidationRequestPublisher = transactionValidationRequestPublisher;
    this.paymentStatusAggregator = paymentStatusAggregator;
  }

  @KafkaListener(topics = "payment-created", groupId = "payment-orchestrator")
  public void handle(PaymentCreatedEvent event) {
    UUID paymentId = UUID.fromString(event.getPaymentId().toString());

    PaymentEntity paymentEntity =
        new PaymentEntity(paymentId, PaymentStatus.PENDING, PaymentStatus.PENDING);

    paymentRepository.save(paymentEntity);

    for (TransactionEvent transaction : event.getTransactions()) {
      UUID transactionId = UUID.fromString(transaction.getTransactionId().toString());

      TransactionEntity transactionEntity =
          new TransactionEntity(transactionId, paymentId, PaymentStatus.PENDING);

      transactionRepository.save(transactionEntity);
    }

    PaymentValidationRequest validationRequest = paymentValidationRequestMapper.toEvent(event);

    paymentValidationRequestPublisher.publish(validationRequest);

    for (TransactionEvent transaction : event.getTransactions()) {
      TransactionValidationRequest request =
          transactionValidationRequestMapper.toEvent(transaction, event.getCurrency());

      transactionValidationRequestPublisher.publish(request);
    }

    System.out.println("Saved payment: " + paymentId + " with status PENDING");
  }

  @KafkaListener(topics = "payment-validation-result", groupId = "payment-orchestrator")
  public void handleValidationResult(PaymentValidationResult result) {
    UUID paymentId = UUID.fromString(result.getPaymentId().toString());

    PaymentEntity payment =
        paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new IllegalStateException("Payment not found: " + paymentId));

    PaymentStatus status = PaymentStatus.valueOf(result.getStatus().toString());

    payment.setPaymentValidationStatus(status);

    paymentRepository.save(payment);

    paymentStatusAggregator.updateFinalPaymentStatus(paymentId);

    System.out.println("Updated payment validation status: " + paymentId + " to " + status);
  }

  @KafkaListener(topics = "transaction-validation-result", groupId = "payment-orchestrator")
  public void handleTransactionValidationResult(TransactionValidationResult result) {

    UUID transactionId = UUID.fromString(result.getTransactionId().toString());

    TransactionEntity transaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () -> new IllegalStateException("Transaction not found: " + transactionId));

    PaymentStatus status = PaymentStatus.valueOf(result.getStatus().toString());

    transaction.setStatus(status);

    transactionRepository.save(transaction);

    paymentStatusAggregator.updateFinalPaymentStatus(transaction.getPaymentId());

    System.out.println("Updated transaction validation status: " + transactionId + " to " + status);
  }
}
