package com.korneliawolniak.paymentprocessing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.korneliawolniak.paymentprocessing.persistence.PaymentEntity;
import com.korneliawolniak.paymentprocessing.persistence.PaymentRepository;
import com.korneliawolniak.paymentprocessing.persistence.PaymentStatus;
import com.korneliawolniak.paymentprocessing.persistence.TransactionEntity;
import com.korneliawolniak.paymentprocessing.persistence.TransactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaymentStatusAggregatorTest {

  private final PaymentRepository paymentRepository = Mockito.mock(PaymentRepository.class);

  private final TransactionRepository transactionRepository =
      Mockito.mock(TransactionRepository.class);

  private final PaymentStatusAggregator aggregator =
      new PaymentStatusAggregator(paymentRepository, transactionRepository);

  @Test
  void shouldSetPaymentToOkWhenPaymentAndAllTransactionsAreOk() {
    UUID paymentId = UUID.randomUUID();

    PaymentEntity payment = new PaymentEntity(paymentId, PaymentStatus.PENDING, PaymentStatus.OK);

    TransactionEntity transaction1 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.OK);

    TransactionEntity transaction2 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.OK);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    when(transactionRepository.findByPaymentId(paymentId))
        .thenReturn(List.of(transaction1, transaction2));

    aggregator.updateFinalPaymentStatus(paymentId);

    assertEquals(PaymentStatus.OK, payment.getStatus());

    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldSetPaymentToNotOkWhenPaymentValidationIsNotOk() {
    UUID paymentId = UUID.randomUUID();

    PaymentEntity payment =
        new PaymentEntity(paymentId, PaymentStatus.PENDING, PaymentStatus.NOT_OK);

    TransactionEntity transaction =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.OK);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    when(transactionRepository.findByPaymentId(paymentId)).thenReturn(List.of(transaction));

    aggregator.updateFinalPaymentStatus(paymentId);

    assertEquals(PaymentStatus.NOT_OK, payment.getStatus());

    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldSetPaymentToNotOkWhenAnyTransactionIsNotOk() {
    UUID paymentId = UUID.randomUUID();

    PaymentEntity payment = new PaymentEntity(paymentId, PaymentStatus.PENDING, PaymentStatus.OK);

    TransactionEntity transaction1 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.OK);

    TransactionEntity transaction2 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.NOT_OK);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    when(transactionRepository.findByPaymentId(paymentId))
        .thenReturn(List.of(transaction1, transaction2));

    aggregator.updateFinalPaymentStatus(paymentId);

    assertEquals(PaymentStatus.NOT_OK, payment.getStatus());

    verify(paymentRepository).save(payment);
  }

  @Test
  void shouldKeepPaymentPendingWhenAnyTransactionIsPending() {
    UUID paymentId = UUID.randomUUID();

    PaymentEntity payment = new PaymentEntity(paymentId, PaymentStatus.PENDING, PaymentStatus.OK);

    TransactionEntity transaction1 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.OK);

    TransactionEntity transaction2 =
        new TransactionEntity(UUID.randomUUID(), paymentId, PaymentStatus.PENDING);

    when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

    when(transactionRepository.findByPaymentId(paymentId))
        .thenReturn(List.of(transaction1, transaction2));

    aggregator.updateFinalPaymentStatus(paymentId);

    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }
}
