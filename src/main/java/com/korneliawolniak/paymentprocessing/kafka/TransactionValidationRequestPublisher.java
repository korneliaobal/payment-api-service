package com.korneliawolniak.paymentprocessing.kafka;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidationRequestPublisher {

  private static final String TOPIC = "transaction-validation-request";

  private final KafkaTemplate<String, TransactionValidationRequest> kafkaTemplate;

  public TransactionValidationRequestPublisher(
      KafkaTemplate<String, TransactionValidationRequest> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void publish(TransactionValidationRequest event) {
    kafkaTemplate.send(TOPIC, event.getPaymentId().toString(), event);
  }
}
