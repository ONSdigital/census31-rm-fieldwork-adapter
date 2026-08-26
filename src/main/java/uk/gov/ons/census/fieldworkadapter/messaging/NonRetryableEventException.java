package uk.gov.ons.census.fieldworkadapter.messaging;

public class NonRetryableEventException extends RuntimeException {
  public NonRetryableEventException(String message) {
    super(message);
  }
}
