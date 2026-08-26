package uk.gov.ons.census.fieldworkadapter.messaging;

public class PublishFailedException extends RuntimeException {
  public PublishFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
