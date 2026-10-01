package com.korneliawolniak.paymentprocessing.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.korneliawolniak.paymentprocessing.avro.PaymentValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.PaymentValidationResult;
import com.korneliawolniak.paymentprocessing.kafka.PaymentValidationResultPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class PaymentCheckerTest {

  private final PaymentValidationResultPublisher publisher =
      Mockito.mock(PaymentValidationResultPublisher.class);

  private final PaymentChecker paymentChecker = new PaymentChecker(publisher);

  @Test
  void shouldPublishOkForValidPayment() {
    PaymentValidationRequest request =
        PaymentValidationRequest.newBuilder()
            .setPaymentId("payment-1")
            .setDebtorName("Finance Department")
            .setDebtorAccountNumber("PL61109010140000071219812874")
            .setCurrency("PLN")
            .setTransactionCount(2)
            .build();

    paymentChecker.handle(request);

    ArgumentCaptor<PaymentValidationResult> captor =
        ArgumentCaptor.forClass(PaymentValidationResult.class);

    verify(publisher).publish(captor.capture());

    PaymentValidationResult result = captor.getValue();

    assertEquals("payment-1", result.getPaymentId().toString());
    assertEquals("OK", result.getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenDebtorNameIsBlank() {
    PaymentValidationRequest request =
        PaymentValidationRequest.newBuilder()
            .setPaymentId("payment-1")
            .setDebtorName("")
            .setDebtorAccountNumber("PL61109010140000071219812874")
            .setCurrency("PLN")
            .setTransactionCount(2)
            .build();

    paymentChecker.handle(request);

    ArgumentCaptor<PaymentValidationResult> captor =
        ArgumentCaptor.forClass(PaymentValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenDebtorAccountNumberIsInvalid() {
    PaymentValidationRequest request =
        PaymentValidationRequest.newBuilder()
            .setPaymentId("payment-1")
            .setDebtorName("Finance Department")
            .setDebtorAccountNumber("INVALID")
            .setCurrency("PLN")
            .setTransactionCount(2)
            .build();

    paymentChecker.handle(request);

    ArgumentCaptor<PaymentValidationResult> captor =
        ArgumentCaptor.forClass(PaymentValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenTransactionCountExceedsFive() {
    PaymentValidationRequest request =
        PaymentValidationRequest.newBuilder()
            .setPaymentId("payment-1")
            .setDebtorName("Finance Department")
            .setDebtorAccountNumber("PL61109010140000071219812874")
            .setCurrency("PLN")
            .setTransactionCount(6)
            .build();

    paymentChecker.handle(request);

    ArgumentCaptor<PaymentValidationResult> captor =
        ArgumentCaptor.forClass(PaymentValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
  }
}
