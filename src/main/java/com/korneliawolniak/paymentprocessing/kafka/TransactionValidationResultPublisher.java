package com.korneliawolniak.paymentprocessing.kafka;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidationResultPublisher {

  private static final String TOPIC = "transaction-validation-result";

  private final KafkaTemplate<String, TransactionValidationResult> kafkaTemplate;

  public TransactionValidationResultPublisher(
      KafkaTemplate<String, TransactionValidationResult> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(TransactionValidationResult event) {
    kafkaTemplate.send(TOPIC, event.getPaymentId().toString(), event);
  }
}
