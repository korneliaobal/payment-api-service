package com.korneliawolniak.paymentprocessing.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class PaymentEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentValidationStatus;

  protected PaymentEntity() {}

  public PaymentEntity(UUID id, PaymentStatus status, PaymentStatus paymentValidationStatus) {
    this.id = id;
    this.status = status;
    this.paymentValidationStatus = paymentValidationStatus;
  }

  public UUID getId() {
    return id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }

  public PaymentStatus getPaymentValidationStatus() {
    return paymentValidationStatus;
  }

  public void setPaymentValidationStatus(PaymentStatus paymentValidationStatus) {
    this.paymentValidationStatus = paymentValidationStatus;
  }
}
