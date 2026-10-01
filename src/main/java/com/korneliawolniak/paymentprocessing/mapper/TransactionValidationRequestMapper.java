package com.korneliawolniak.paymentprocessing.mapper;

import com.korneliawolniak.paymentprocessing.avro.TransactionEvent;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidationRequestMapper {

  public TransactionValidationRequest toEvent(TransactionEvent transaction, CharSequence currency) {

    return TransactionValidationRequest.newBuilder()
        .setTransactionId(transaction.getTransactionId())
        .setPaymentId(transaction.getPaymentId())
        .setCreditorName(transaction.getCreditorName())
        .setCreditorAccountNumber(transaction.getCreditorAccountNumber())
        .setAmount(transaction.getAmount())
        .setCurrency(currency)
        .build();
  }
}
