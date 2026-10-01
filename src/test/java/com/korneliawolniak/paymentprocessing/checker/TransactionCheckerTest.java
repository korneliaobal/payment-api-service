package com.korneliawolniak.paymentprocessing.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import com.korneliawolniak.paymentprocessing.kafka.TransactionValidationResultPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TransactionCheckerTest {

  private final TransactionValidationResultPublisher publisher =
      Mockito.mock(TransactionValidationResultPublisher.class);

  private final TransactionChecker transactionChecker = new TransactionChecker(publisher);

  @Test
  void shouldPublishOkForValidTransaction() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    TransactionValidationResult result = captor.getValue();

    assertEquals("transaction-1", result.getTransactionId().toString());
    assertEquals("payment-1", result.getPaymentId().toString());
    assertEquals("OK", result.getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenCreditorNameIsBlank() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenCreditorAccountNumberIsInvalid() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("INVALID")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenAmountIsFiveOrLess() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("5.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }
}
