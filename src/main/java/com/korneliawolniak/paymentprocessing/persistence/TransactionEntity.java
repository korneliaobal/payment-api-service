package com.korneliawolniak.paymentprocessing.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class TransactionEntity {

  @Id private UUID id;

  private UUID paymentId;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  protected TransactionEntity() {}

  public TransactionEntity(UUID id, UUID paymentId, PaymentStatus status) {
    this.id = id;
    this.paymentId = paymentId;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }
}
